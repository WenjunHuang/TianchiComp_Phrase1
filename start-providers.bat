java -jar ^
   -Xms512M ^
   -Xmx512M ^
   -DRUN_TYPE=provider-small ^
   -DACTOR_SYSTEM_HOST=localhost ^
   -DACTOR_SYSTEM_PORT=2552 ^
   -DETCD_HOST=localhost ^
   -DETCD_PORT=2379 ^
   -DDUBBO_PROVIDER_HOST=localhost ^
   -DDUBBO_PROVIDER_PORT=20880 ^
   -DDUBBO_CONNECTION_COUNT=2 ^
   -DDUBBO_COUNT_PER_CONNECTION=100 ^
   ./target/scala-2.12/mesh-agent.jar

