# franklin-fragment-converter
A converter for content fragments to SQL

## Build

    mvn clean install


## Running

The following will convert the fragments defines in the file `fragments.json`
and store the SQL statements in the file `sql.txt`:

    java -cp target/fragments-converter-*.jar \
      com.adobe.franklin.fragments.converter.ConvertFragmentsToSQL \
      --fileName fragments.json \
      --maxRows 1000 \
      > sql.txt