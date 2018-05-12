java -jar ^
       -Xms1536M ^
       -Xmx1536M ^
       -DRUN_TYPE=consumer ^
       -DACTOR_SYSTEM_HOST=localhost ^
       -DACTOR_SYSTEM_PORT=2553 ^
       -DETCD_HOST=localhost ^
       -DETCD_PORT=2379 ^
       -DHTTP_LISTENING_PORT=20000 ^
       ./target/scala-2.12/mesh-agent.jar
