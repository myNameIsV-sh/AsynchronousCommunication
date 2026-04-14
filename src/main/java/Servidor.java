import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class Servidor {

    private final int porta;
    private ServerSocket serverSocket;

    public Servidor(int porta) {
        this.porta = porta;
    }

    public void iniciar() {
        try {
            serverSocket = new ServerSocket(porta);
            System.out.println("Porta " + porta + " aberta!");

            while (true) {
                Socket cliente = serverSocket.accept();
                System.out.println("Nova conexão com o cliente " +
                        cliente.getInetAddress().getHostAddress());

                Thread thread = new Thread(() -> atenderCliente(cliente));
                thread.start();
            }

        } catch (IOException e) {
            System.err.println("Erro ao iniciar o servidor: " + e.getMessage());
        }
    }

    private void atenderCliente(Socket cliente) {
        try {
            Scanner s = new Scanner(cliente.getInputStream());

            while (s.hasNextLine()) {
                System.out.println(s.nextLine());
            }

            s.close();
            cliente.close();

        } catch (IOException e) {
            System.err.println("Erro ao atender cliente: " + e.getMessage());
        }
    }

    public void fechar() {
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            System.err.println("Erro ao fechar o servidor: " + e.getMessage());
        }
    }
}