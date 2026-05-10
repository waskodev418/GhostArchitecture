# Web server and Client model

## pulse key
In order to make this model work, it is NECESSARY to start the `pulse_daemon.exe` located inside
`function/daemon`.
If the process is not running then the two servers - this and the *Data server* - won't be able to
share the server keys correctly.

### Compile the file
Use `g++ -O3 ./daemon.cpp -o pulse_daemon.exe -I. -lpthread` to compile the `daemon.cpp` file inside the `daemon` folder

---