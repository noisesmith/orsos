# orsos

Clojure program to build a datomic database with data available from the Oregon Secretary of State

## Usage

requires the csv files from ORSoS to be available under resources/orsos/

right now it expects the transaction csv files to be under `resources/orsos/transactions` and the committee csv files to be under `resources/orsos/committees`

    lein run

loads the csvs into the in-memory database and then runs the query specified in `src/org/noisesmith/orsos.clj:-main`

### Bugs

## License

Copyright © 2014 Justin Glenn Smith noisesmith@gmail.com

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
