# BAIC USB Diagnóstico

App Android independiente para inspeccionar los dispositivos USB que Android expone en modo host. No es una app oficial de BAIC. No duplica pantalla, no habilita Android Auto y no diagnostica la ECU ni confirma compatibilidad con el vehículo.

## Instalar y probar
1. Descargá BAIC-USB-diagnostico.apk en Releases e instalalo en Android 6 o posterior.
2. Abrí la app y conectá un dispositivo mediante un cable de datos y, si corresponde, adaptador OTG.
3. Tocá Actualizar. Para cada dispositivo podés solicitar permiso y luego probar apertura.
4. Compartí el informe para revisar identificadores, interfaces y endpoints.

Si el auto actúa como host y el teléfono como periférico, la API host del teléfono puede mostrar cero dispositivos aunque esté conectado y cargando. Cero dispositivos no demuestra una falla del puerto ni del cable. La app también muestra accesorios Android expuestos por el sistema, pero no abre conexiones de accesorios ni implementa protocolos del fabricante.

La prueba abre y cierra la conexión USB; no reclama interfaces ni envía comandos al vehículo. No requiere Internet ni permiso de almacenamiento. El informe puede incluir nombres e identificadores de dispositivos: revisalo antes de compartirlo.

## Compilación
JDK 17, Gradle 8.9, Android SDK 35, Android Gradle Plugin 8.7.3.
Con Gradle 8.9 instalado: `gradle --no-daemon :app:assembleDebug :app:lintDebug`.
GitHub Actions instala las herramientas, compila, ejecuta lint, verifica firma/identidad del APK y publica un artefacto y una release por ejecución exitosa de main.

El APK está firmado con la clave de depuración temporal del ejecutor, para pruebas. Entre ejecuciones puede ser necesario desinstalar la versión anterior antes de instalar una nueva. No se incluye una clave privada de distribución.

## Alcance de verificación
Compilar y validar el APK no prueba la conexión física con un vehículo. Probá el permiso denegado y concedido, desconexión, reconexión y compartir informe en tu teléfono. No hay compatibilidad BAIC específica confirmada.
