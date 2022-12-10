# franklin-fragment-converter

A converter for content fragments stored in Apache Jackrabbit Oak to a relational database.

Currently supported databases are PostgreSQL and SQLite, or, for testing purposes, creation of a SQL script.

## Build

    mvn clean install

## Running

The following will convert the fragments defines in the file `fragments.json`
and store the SQL statements in the file `sql.txt` (at most 1000 lines):

    java -cp "target/*:target/lib/*" \
      com.adobe.franklin.fragments.converter.ConvertFragmentsToSQL \
      --fileName fragments.json \
      --maxRows 1000 \
      > sql.txt

To directly run all statements against a SQL database:

    java -cp "target/*:target/lib/*" \
      com.adobe.franklin.fragments.converter.ConvertFragmentsToSQL \
      --fileName fragments.json \
      --jdbcUrl jdbc:postgresql:test \
      --jdbcUser test

To extract from a local segment store and populate a database:

    java -cp "target/*:target/lib/*" \         
      com.adobe.franklin.fragments.converter.ConvertFragmentsToSQL \
      --oakRepo crx-quickstart/repository \
      --jdbcUrl jdbc:postgresql:test \
      --jdbcUser test
      
To populate a SQLite database:

    java -cp "target/*:target/lib/*" \
      com.adobe.franklin.fragments.converter.ConvertFragmentsToSQL \
      --oakRepo crx-quickstart/repository \
      --jdbcUrl jdbc:sqlite:test.sqlite \
      --jdbcUser test
