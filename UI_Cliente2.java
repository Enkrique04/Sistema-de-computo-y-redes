import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.net.*;

// Clase principal
public class UI_Cliente extends JFrame implements ActionListener {

    // Clave secreta AES (16 bytes)
    private static final String CLAVE_SECRETA = "1234567890abcdef";

    // Método para encriptar usando AES
    public static String encriptarAES(String mensaje) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(CLAVE_SECRETA.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedBytes = cipher.doFinal(mensaje.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    // Método para desencriptar usando AES
    public static String desencriptarAES(String mensajeEncriptado) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(CLAVE_SECRETA.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decodedBytes = Base64.getDecoder().decode(mensajeEncriptado);
        byte[] decryptedBytes = cipher.doFinal(decodedBytes);
        return new String(decryptedBytes);
    }

    // Componentes de la interfaz
    private JPanel panel;
    private JTextArea textArea;
    private JScrollPane jScrollPane;
    private JLabel lblUsuario, lblIP, lblPuerto, lblNotificacion;
    private JTextField txtUsuario, txtIP, txtPuerto, txtMensaje;
    private JButton btnConectar, btnEnviar;

    // Comunicación
    private Socket servidor;
    private PrintWriter out;
    private String ip, usuario, mensaje;
    private int puerto;

    private boolean conectado = false;
    private Monitor monitor;
    private Thread t;

    // Constructor
    public UI_Cliente() {
        this.setTitle("MiCliente");
        panel = new JPanel();
        panel.setLayout(null);

        lblUsuario = new JLabel("Usuario:");
        txtUsuario = new JTextField();
        lblIP = new JLabel("IP destino:");
        txtIP = new JTextField();
        lblPuerto = new JLabel("Puerto:");
        txtPuerto = new JTextField();
        lblNotificacion = new JLabel("Desconectado");
        btnConectar = new JButton("Conectar");
        btnConectar.setBackground(Color.decode("#FF7171"));
        textArea = new JTextArea("Mensajes:");
        textArea.setEditable(false);
        jScrollPane = new JScrollPane(textArea);
        txtMensaje = new JTextField();
        txtMensaje.setEnabled(false);
        btnEnviar = new JButton("Enviar");
        btnEnviar.setEnabled(false);

        panel.add(lblUsuario);
        panel.add(txtUsuario);
        panel.add(lblIP);
        panel.add(txtIP);
        panel.add(lblPuerto);
        panel.add(txtPuerto);
        panel.add(lblNotificacion);
        panel.add(btnConectar);
        panel.add(jScrollPane);
        panel.add(txtMensaje);
        panel.add(btnEnviar);

        lblUsuario.setBounds(50, 25, 100, 20);
        txtUsuario.setBounds(130, 25, 200, 20);
        lblIP.setBounds(50, 50, 100, 20);
        txtIP.setBounds(130, 50, 200, 20);
        lblPuerto.setBounds(50, 75, 100, 20);
        txtPuerto.setBounds(130, 75, 50, 20);
        lblNotificacion.setBounds(200, 75, 200, 20);
        btnConectar.setBounds(375, 25, 150, 50);
        jScrollPane.setBounds(50, 110, 525, 300);
        txtMensaje.setBounds(50, 415, 405, 75);
        btnEnviar.setBounds(455, 415, 120, 75);

        this.add(panel);
        this.setDefaultCloseOperation(this.EXIT_ON_CLOSE);
        this.setSize(600, 500);
        this.setVisible(true);

        btnConectar.addActionListener(this);
        btnEnviar.addActionListener(this);
    }

    // Manejo de eventos
    public void actionPerformed(ActionEvent event) {
        if (event.getSource() == btnConectar) {
            if (!conectado) {
                conectar();
            } else {
                desconectar();
            }
        } else if (event.getSource() == btnEnviar) {
            enviar();
        }
    }

    // Conexión al servidor
    public void conectar() {
        System.out.println("Conectando...");
        try {
            ip = txtIP.getText();
            puerto = Integer.parseInt(txtPuerto.getText());
            servidor = new Socket(ip, puerto);
            monitor = new Monitor(servidor, textArea);
            t = new Thread(monitor);
            t.start();
            out = new PrintWriter(servidor.getOutputStream(), true);
            usuario = txtUsuario.getText();

            txtUsuario.setEnabled(false);
            txtIP.setEnabled(false);
            txtPuerto.setEnabled(false);
            btnConectar.setText("Desconectar");
            btnConectar.setBackground(Color.decode("#64FF69"));
            txtMensaje.setEnabled(true);
            btnEnviar.setEnabled(true);
            txtMensaje.requestFocusInWindow();

            lblNotificacion.setText("Conectado");
            conectado = true;
        } catch (Exception err) {
            err.printStackTrace();
            lblNotificacion.setText("Error");
        }
    }

    // Desconexión del servidor
    public void desconectar() {
        try {
            out.println("DSCNCTR");
            t.interrupt();
            servidor.close();
            txtUsuario.setEnabled(true);
            txtIP.setEnabled(true);
            txtPuerto.setEnabled(true);
            btnConectar.setText("Conectar");
            btnConectar.setBackground(Color.decode("#FF7171"));
            txtMensaje.setEnabled(false);
            btnEnviar.setEnabled(false);

            lblNotificacion.setText("Desconectado");
            conectado = false;
        } catch (Exception err) {
            err.printStackTrace();
            lblNotificacion.setText("Error");
        }
    }

    // Enviar mensaje al servidor
    public void enviar() {
        try {
            mensaje = txtMensaje.getText();
            String completo = usuario + ": " + mensaje;
            String mensajeEncriptado = encriptarAES(completo);

            System.out.println(mensajeEncriptado);
            out.println(mensajeEncriptado);
            txtMensaje.setText("");
            txtMensaje.requestFocusInWindow();
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    // Método principal
    public static void main(String[] args) {
        new UI_Cliente();
    }
}

// Clase para monitorear mensajes entrantes
class Monitor implements Runnable {

    private BufferedReader in;
    private Socket servidor;
    private JTextArea textArea;
    private String msg;

    public Monitor(Socket servidor, JTextArea textArea) {
        this.servidor = servidor;
        this.textArea = textArea;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(servidor.getInputStream()));

            while ((msg = in.readLine()) != null) {
                try {
                    String mensajeDesencriptado = UI_Cliente.desencriptarAES(msg);
                    System.out.println(mensajeDesencriptado);
                    textArea.setText(textArea.getText() + "\n" + mensajeDesencriptado);
                } catch (IllegalArgumentException e) {
                    // Si no es Base64 válido, solo lo muestra tal cual
                    textArea.setText(textArea.getText() + "\n(Mensaje no encriptado): " + msg);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
