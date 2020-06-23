# Instructions
## OSRM-Backend
Um die Distanzen zwischen den einzelnen MSG- Standorten erneut zu berechen, starten Sie das OSRM-Backend mit dem folgenden Docker-Befehl:

`docker run -t -v "${PWD}\data:/data" osrm/osrm-backend osrm-extract -p /opt/car.lua /data/germany-latest.osm.pbf`

In dem Ordner data muss sich das aktuelle Open Street Map Abbild von Deutschland befinden. Dieses kann beispielsweise [hier](http://download.geofabrik.de/europe/germany) heruntergeladen werden 

`docker run -t -v "${PWD}\data:/data" osrm/osrm-backend osrm-partition /data/germany-latest.osrm `

`docker run -t -v "${PWD}\data:/data" osrm/osrm-backend osrm-customize /data/germany-latest.osrm `

`docker run -t -i -p 5000:5000 -v "${PWD}\data:/data" osrm/osrm-backend osrm-routed --algorithm mld /data/germany-latest.osrm `
