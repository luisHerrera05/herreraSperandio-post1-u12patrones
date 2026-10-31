#!/usr/bin/env bash
# Evidencia funcional (la aplicacion debe estar corriendo en :8080)
B=http://localhost:8080/api/solicitudes
H='Content-Type: application/json'
echo "== VIAJE (3 dias, 120000) =="
curl -s -X POST $B -H "$H" -d '{"empleadoId":"E1","categoria":"VIAJE","montoSolicitado":120000,"fechaGasto":"2026-08-10","diasViaje":3,"documentosSoporte":true}'; echo
echo "== REPRESENTACION (1.500.000) =="
curl -s -X POST $B -H "$H" -d '{"empleadoId":"E2","categoria":"REPRESENTACION","montoSolicitado":1500000,"fechaGasto":"2026-08-10","diasViaje":0,"documentosSoporte":true}'; echo
echo "== CAPACITACION (100.000 -> 80.000) =="
curl -s -X POST $B -H "$H" -d '{"empleadoId":"E3","categoria":"CAPACITACION","montoSolicitado":100000,"fechaGasto":"2026-08-10","diasViaje":0,"documentosSoporte":true}'; echo
echo "== SIN DOCUMENTOS (400) =="
curl -s -i -X POST $B -H "$H" -d '{"empleadoId":"E4","categoria":"VIAJE","montoSolicitado":100000,"fechaGasto":"2026-08-10","diasViaje":2,"documentosSoporte":false}'; echo
echo "== DUPLICADO (400: misma categoria y periodo que E1) =="
curl -s -i -X POST $B -H "$H" -d '{"empleadoId":"E1","categoria":"VIAJE","montoSolicitado":90000,"fechaGasto":"2026-08-25","diasViaje":2,"documentosSoporte":true}'; echo
echo "== APROBAR solicitud 1 (dispara los 3 observadores; mirar la consola) =="
curl -s -X PUT $B/1/estado -H "$H" -d '"APROBADA"'; echo
echo "== RECHAZAR solicitud 2 (Contabilidad NO actua) =="
curl -s -X PUT $B/2/estado -H "$H" -d '"RECHAZADA"'; echo
