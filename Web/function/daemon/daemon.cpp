#include <iostream>
#include <fstream>
#include <string>
#include <sstream>
#include <iomanip>
#include <vector>
#include <thread>
#include <chrono>
#include <algorithm>
#include <cstdint>
#include "nlohmann/json.hpp"

using Json = nlohmann::json;

class SHA256 {
private:
    uint32_t state[8];
    uint64_t bitlen;
    uint8_t data[64];
    uint32_t datalen;

    uint32_t rotl(uint32_t x, uint32_t n) { return (x << n) | (x >> (32 - n)); }
    uint32_t rotr(uint32_t x, uint32_t n) { return (x >> n) | (x << (32 - n)); }
    uint32_t ch(uint32_t x, uint32_t y, uint32_t z) { return (x & y) ^ (~x & z); }
    uint32_t maj(uint32_t x, uint32_t y, uint32_t z) { return (x & y) ^ (x & z) ^ (y & z); }
    uint32_t sig0(uint32_t x) { return rotr(x, 7) ^ rotr(x, 18) ^ (x >> 3); }
    uint32_t sig1(uint32_t x) { return rotr(x, 17) ^ rotr(x, 19) ^ (x >> 10); }
    uint32_t EP0(uint32_t x) { return rotr(x, 2) ^ rotr(x, 13) ^ rotr(x, 22); }
    uint32_t EP1(uint32_t x) { return rotr(x, 6) ^ rotr(x, 11) ^ rotr(x, 25); }

    const uint32_t k[64] = {
        0x428a2f98,0x71374491,0xb5c0fbcf,0xe9b5dba5,0x3956c25b,0x59f111f1,0x923f82a4,0xab1c5ed5,
        0xd807aa98,0x12835b01,0x243185be,0x550c7dc3,0x72be5d74,0x80deb1fe,0x9bdc06a7,0xc19bf174,
        0xe49b69c1,0xefbe4786,0x0fc19dc6,0x240ca1cc,0x2de92c6f,0x4a7484aa,0x5cb0a9dc,0x76f988da,
        0x983e5152,0xa831c66d,0xb00327c8,0xbf597fc7,0xc6e00bf3,0xd5a79147,0x06ca6351,0x14292967,
        0x27b70a85,0x2e1b2138,0x4d2c6dfc,0x53380d13,0x650a7354,0x766a0abb,0x81c2c92e,0x92722c85,
        0xa2bfe8a1,0xa81a664b,0xc24b8b70,0xc76c51a3,0xd192e819,0xd6990624,0xf40e3585,0x106aa070,
        0x19a4c116,0x1e376c08,0x2748774c,0x34b0bcb5,0x391c0cb3,0x4ed8aa4a,0x5b9cca4f,0x682e6ff3,
        0x748f82ee,0x78a5636f,0x84c87814,0x8cc70208,0x90befffa,0xa4506ceb,0xbef9a3f7,0xc67178f2
    };

    void transform() {
        uint32_t a, b, c, d, e, f, g, h, i, j, t1, t2, m[64];
        for (i = 0, j = 0; i < 16; ++i, j += 4)
            m[i] = (data[j] << 24) | (data[j + 1] << 16) | (data[j + 2] << 8) | (data[j + 3]);
        for (; i < 64; ++i)
            m[i] = sig1(m[i - 2]) + m[i - 7] + sig0(m[i - 15]) + m[i - 16];
        a = state[0]; b = state[1]; c = state[2]; d = state[3];
        e = state[4]; f = state[5]; g = state[6]; h = state[7];
        for (i = 0; i < 64; ++i) {
            t1 = h + EP1(e) + ch(e, f, g) + k[i] + m[i];
            t2 = EP0(a) + maj(a, b, c);
            h = g; g = f; f = e; e = d + t1; d = c; c = b; b = a; a = t1 + t2;
        }
        state[0] += a; state[1] += b; state[2] += c; state[3] += d;
        state[4] += e; state[5] += f; state[6] += g; state[7] += h;
    }

public:
    SHA256() : bitlen(0), datalen(0) {
        state[0] = 0x6a09e667; state[1] = 0xbb67ae85; state[2] = 0x3c6ef372; state[3] = 0xa54ff53a;
        state[4] = 0x510e527f; state[5] = 0x9b05688c; state[6] = 0x1f83d9ab; state[7] = 0x5be0cd19;
    }

    void update(const uint8_t* str, size_t len) {
        for (size_t i = 0; i < len; ++i) {
            data[datalen] = str[i];
            datalen++;
            if (datalen == 64) {
                transform();
                bitlen += 512;
                datalen = 0;
            }
        }
    }

    void final(uint8_t* hash) {
        uint32_t i = datalen;
        if (datalen < 56) {
            data[i++] = 0x80;
            while (i < 56) data[i++] = 0x00;
        } else {
            data[i++] = 0x80;
            while (i < 64) data[i++] = 0x00;
            transform();
            memset(data, 0, 56);
        }
        bitlen += datalen * 8;
        data[63] = bitlen; data[62] = bitlen >> 8; data[61] = bitlen >> 16; data[60] = bitlen >> 24;
        data[59] = bitlen >> 32; data[58] = bitlen >> 40; data[57] = bitlen >> 48; data[56] = bitlen >> 56;
        transform();
        for (i = 0; i < 4; ++i) {
            hash[i]      = (state[0] >> (24 - i * 8)) & 0x000000ff;
            hash[i + 4]  = (state[1] >> (24 - i * 8)) & 0x000000ff;
            hash[i + 8]  = (state[2] >> (24 - i * 8)) & 0x000000ff;
            hash[i + 12] = (state[3] >> (24 - i * 8)) & 0x000000ff;
            hash[i + 16] = (state[4] >> (24 - i * 8)) & 0x000000ff;
            hash[i + 20] = (state[5] >> (24 - i * 8)) & 0x000000ff;
            hash[i + 24] = (state[6] >> (24 - i * 8)) & 0x000000ff;
            hash[i + 28] = (state[7] >> (24 - i * 8)) & 0x000000ff;
        }
    }
};

std::string hmacSha256(const std::string& key, const std::string& data) {
    uint8_t k_ipad[64], k_opad[64], key_arr[64] = {0};
    
    if (key.length() > 64) {
        SHA256 sha;
        sha.update((uint8_t*)key.c_str(), key.length());
        sha.final(key_arr);
    } else {
        memcpy(key_arr, key.c_str(), key.length());
    }

    for (int i = 0; i < 64; i++) {
        k_ipad[i] = key_arr[i] ^ 0x36;
        k_opad[i] = key_arr[i] ^ 0x5c;
    }

    uint8_t inner_hash[32], outer_hash[32];
    SHA256 inner;
    inner.update(k_ipad, 64);
    inner.update((uint8_t*)data.c_str(), data.length());
    inner.final(inner_hash);

    SHA256 outer;
    outer.update(k_opad, 64);
    outer.update(inner_hash, 32);
    outer.final(outer_hash);

    std::stringstream ss;
    for(int i = 0; i < 32; i++) ss << std::hex << std::setw(2) << std::setfill('0') << (int)outer_hash[i];
    return ss.str();
}

void writePulseFile(const std::string& path, const std::string& jsonContent) {
    std::string tmp = path + ".tmp";
    std::ofstream ofs(tmp);
    if (ofs.is_open()) {
        ofs << jsonContent;
        ofs.close();
        std::remove(path.c_str());
        std::rename(tmp.c_str(), path.c_str());
    }
}

Json readPulseFile(const std::string& path) {
    std::ifstream ifs(path);
    if (!ifs.is_open()) return nullptr;
    Json j;
    try { ifs >> j; } catch (...) { return nullptr; }
    return j;
}

int getNextKey(std::string &secret_number, std::string &key1, std::string &key2, std::string &key3) {
    int reminder = (int)((unsigned char)secret_number.at(0)) % 3;
    std::string prk;

    if(reminder == 0) prk = hmacSha256(hmacSha256(key1, key2), hmacSha256(key3, secret_number));
    else if(reminder == 1) prk = hmacSha256(hmacSha256(key3, secret_number), hmacSha256(key2, key1));
    else prk = hmacSha256(hmacSha256(key2, key1), hmacSha256(secret_number, key3));

    //since we will use a derivation from the main key in the ciphers,
    //an unfixed length increments entropy much more than a fixed one
    int offset = ((unsigned char)key3[5]) % 22;
    int size = ((unsigned char)key2.back()) % 22 + 20;
    std::string reverse_key = prk.substr(offset, size);

    int val1 = (int)((unsigned char)key2[(unsigned char)key1.back() % key2.size()]);
    int val2 = (int)((unsigned char)key3[(unsigned char)key1[key1.size()-2] % key3.size()]);
    int lifespan = std::max(60, val1 + val2);

    bool isEven = ((unsigned char)secret_number.back() % 2 == 0);
    secret_number = isEven ? hmacSha256(secret_number, key3) : hmacSha256(key3, secret_number);

    key3 = key2; key2 = key1; key1 = "";
    for (int i = (int)reverse_key.length() - 1; i >= 0; i--) key1 += reverse_key.at(i);

    return lifespan;
}

int main() {
    const std::string PATH = "../symmetric_key.json";
    std::string root = "init_root", k1 = "init_k1", k2 = "init_k2", k3 = "init_k3";

    while(true) {
        Json content = readPulseFile(PATH);
        if (!content.is_null() && content.contains("root")) {
            root = content["root"]; k1 = content["key1"]; k2 = content["key2"]; k3 = content["key3"];
        }

        int lifespan = getNextKey(root, k1, k2, k3);

        Json new_content;
        new_content["root"] = root; new_content["key1"] = k1; new_content["key2"] = k2; new_content["key3"] = k3;
        writePulseFile(PATH, new_content.dump());

        std::cout << "Update success key: " << k1 << " Sleep: " << lifespan << "s" << std::endl;
        std::this_thread::sleep_for(std::chrono::seconds(lifespan));
    }
    return 0;
}