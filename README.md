# franklin-fragment-converter
A converter for content fragments to SQL

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
