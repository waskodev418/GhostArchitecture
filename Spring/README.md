# Data server
This is a *model* of the server responsible for the
communication with the DBMS.

## Initialize the database
In order to generate a database to try this out, it is 
recommended to go on `src/main/resources/application.properties`
and change the value of `spring.jpa.hibernate.ddl-auto` to `create`.
This will make JPA automatically generate the DB structure according to 
the java classes: `Veicolo` and `Prenotazione`.

---