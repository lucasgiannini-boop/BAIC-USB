package com.baicusb.diagnostico;

import android.app.Activity;
import androidx.core.content.ContextCompat;
import android.app.PendingIntent;
import android.content.*;
import android.content.pm.PackageManager;
import android.hardware.usb.*;
import android.os.*;
import android.view.View;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    private static final String PERMISSION = "com.baicusb.diagnostico.USB_PERMISSION";
    private UsbManager usb;
    private LinearLayout devices;
    private TextView summary;
    private String report = "";
    private final LinkedHashMap<String, String> events = new LinkedHashMap<>();
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            // Re-read trusted UsbManager state; do not trust broadcast extras.
            if (PERMISSION.equals(intent.getAction())) {
                note("Permiso", "Solicitud finalizada. Revisá el estado del dispositivo y tocá Probar apertura.");
            } else {
                note("Conexión", "Cambió la conexión USB.");
            }
            refresh();
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        usb = (UsbManager) getSystemService(USB_SERVICE);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);
        // Respect system bars with Android 15 edge-to-edge enforcement.
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(pad + insets.getSystemWindowInsetLeft(),
                    pad + insets.getSystemWindowInsetTop(),
                    pad + insets.getSystemWindowInsetRight(),
                    pad + insets.getSystemWindowInsetBottom());
            return insets;
        });
        scroll.addView(root);
        setContentView(scroll);
        TextView title = text("BAIC USB Diagnóstico", 26);
        root.addView(title);
        root.addView(text("Inspección USB · sin duplicación de pantalla", 16));
        root.addView(text("Conectá un cable de datos. Si el teléfono debe actuar como host, puede requerir OTG. Esta app no envía comandos al auto.", 16));
        button(root, "Actualizar dispositivos", v -> refresh());
        button(root, "Compartir informe", v -> {
            refresh();
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            send.putExtra(Intent.EXTRA_SUBJECT, "Informe BAIC USB");
            send.putExtra(Intent.EXTRA_TEXT, report);
            startActivity(Intent.createChooser(send, "Compartir diagnóstico"));
        });
        summary = text("", 16);
        root.addView(summary);
        devices = new LinearLayout(this);
        devices.setOrientation(LinearLayout.VERTICAL);
        root.addView(devices);
        IntentFilter filter = new IntentFilter(PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
        refresh();
    }

    @Override protected void onResume() {
        super.onResume();
        if (devices != null) refresh();
    }
    @Override protected void onDestroy() {
        unregisterReceiver(receiver);
        super.onDestroy();
    }
    private TextView text(String value, int size) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextIsSelectable(true);
        view.setPadding(0, 12, 0, 12);
        return view;
    }
    private void button(LinearLayout parent, String label, View.OnClickListener action) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setOnClickListener(action);
        parent.addView(button);
    }
    private void note(String key, String value) {
        events.put(key, value);
        while (events.size() > 16) events.remove(events.keySet().iterator().next());
    }
    private String safe(String value) { return value == null ? "No informado" : value; }
    private void refresh() {
        devices.removeAllViews();
        StringBuilder out = new StringBuilder("BAIC USB Diagnóstico 1.0\n");
        out.append("Fecha: ").append(new Date()).append("\nAndroid: ")
           .append(Build.VERSION.RELEASE).append(" / API ").append(Build.VERSION.SDK_INT)
           .append("\nTeléfono: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL)
           .append("\nSoporte USB host declarado: ")
           .append(getPackageManager().hasSystemFeature(PackageManager.FEATURE_USB_HOST) ? "Sí" : "No")
           .append("\n");
        if (usb == null) {
            out.append("Servicio USB no disponible.");
        } else {
            try {
                ArrayList<UsbDevice> list = new ArrayList<>(usb.getDeviceList().values());
                Collections.sort(list, (a, b) -> a.getDeviceName().compareTo(b.getDeviceName()));
                out.append("Dispositivos host detectados: ").append(list.size()).append("\n");
                if (list.isEmpty()) {
                    out.append("\nNo hay dispositivos visibles en modo host. El auto puede ser el host y el teléfono un periférico. Esto no confirma un cable o puerto defectuoso.\n");
                }
                for (UsbDevice d : list) {
                    String detail = describe(d);
                    out.append("\n").append(detail);
                    devices.addView(text(detail, 15));
                    if (usb.hasPermission(d)) button(devices, "Probar apertura · " + d.getDeviceName(), v -> probe(d));
                    else button(devices, "Solicitar permiso · " + d.getDeviceName(), v -> permission(d));
                }
                UsbAccessory[] accessories = usb.getAccessoryList();
                out.append("\nAccesorios Android: ").append(accessories == null ? 0 : accessories.length).append("\n");
                if (accessories != null) for (UsbAccessory a : accessories) {
                    out.append(safe(a.getManufacturer())).append(" / ").append(safe(a.getModel()))
                       .append(" / ").append(safe(a.getDescription())).append("\n");
                }
            } catch (RuntimeException e) {
                out.append("No se pudo consultar USB: ").append(e.getClass().getSimpleName()).append("\n");
            }
        }
        if (!events.isEmpty()) {
            out.append("\nÚltimos resultados (históricos; actualizar no repite pruebas):\n");
            for (Map.Entry<String, String> e : events.entrySet()) out.append(e.getKey()).append(": ").append(e.getValue()).append("\n");
        }
        out.append("\nNo prueba compatibilidad BAIC ni duplicación de pantalla.\n");
        report = out.toString();
        summary.setText(report);
    }
    private String describe(UsbDevice d) {
        StringBuilder s = new StringBuilder();
        s.append(safe(d.getProductName())).append(" / ").append(safe(d.getManufacturerName()))
         .append("\nRuta: ").append(d.getDeviceName())
         .append(String.format(Locale.ROOT, "\nVID: %04X · PID: %04X", d.getVendorId(), d.getProductId()))
         .append("\nClase: ").append(d.getDeviceClass())
         .append(" · Subclase: ").append(d.getDeviceSubclass())
         .append(" · Protocolo: ").append(d.getDeviceProtocol())
         .append("\nPermiso: ").append(usb.hasPermission(d) ? "Concedido" : "Sin permiso")
         .append("\nInterfaces: ").append(d.getInterfaceCount()).append("\n");
        for (int i = 0; i < d.getInterfaceCount(); i++) {
            UsbInterface f = d.getInterface(i);
            s.append("  Interfaz ").append(f.getId()).append(" · clase ").append(f.getInterfaceClass())
             .append(" · subclase ").append(f.getInterfaceSubclass()).append(" · protocolo ")
             .append(f.getInterfaceProtocol()).append("\n");
            for (int j = 0; j < f.getEndpointCount(); j++) {
                UsbEndpoint ep = f.getEndpoint(j);
                s.append("    Endpoint ").append(ep.getAddress())
                 .append(ep.getDirection() == UsbConstants.USB_DIR_IN ? " IN" : " OUT")
                 .append(" · tipo ").append(ep.getType()).append(" · paquete ")
                 .append(ep.getMaxPacketSize()).append(" bytes\n");
            }
        }
        return s.toString();
    }
    private void permission(UsbDevice d) {
        try {
            Intent intent = new Intent(PERMISSION).setPackage(getPackageName());
            PendingIntent pending = PendingIntent.getBroadcast(this, d.getDeviceId(), intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            usb.requestPermission(d, pending);
            note(d.getDeviceName(), "Permiso solicitado. Si lo rechazás, podés volver a solicitarlo.");
        } catch (RuntimeException e) {
            note(d.getDeviceName(), "No se pudo solicitar permiso: " + e.getClass().getSimpleName());
        }
        refresh();
    }
    private void probe(UsbDevice d) {
        UsbDeviceConnection connection = null;
        try {
            if (!usb.getDeviceList().containsKey(d.getDeviceName())) {
                note(d.getDeviceName(), "Dispositivo desconectado.");
            } else if (!usb.hasPermission(d)) {
                note(d.getDeviceName(), "Sin permiso. Solicitá permiso y repetí la prueba.");
            } else {
                connection = usb.openDevice(d);
                note(d.getDeviceName(), connection == null
                        ? "Android no pudo abrir el dispositivo."
                        : "Apertura correcta. Se cierra la conexión sin enviar comandos. No confirma compatibilidad con el vehículo.");
            }
        } catch (RuntimeException e) {
            note(d.getDeviceName(), "Error de apertura: " + e.getClass().getSimpleName());
        } finally {
            if (connection != null) connection.close();
        }
        refresh();
    }
}
