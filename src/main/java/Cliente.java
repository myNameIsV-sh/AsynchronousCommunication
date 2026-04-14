import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class Cliente {

    private final String host;
    private final int porta;
    private Socket socket;
    private PrintStream saida;

    public Cliente(String host, int porta) {
        this.host = host;
        this.porta = porta;
    }

    public void conectar() {
        try {
            socket = new Socket(host, porta);
            saida = new PrintStream(socket.getOutputStream());
            System.out.println("O cliente se conectou ao servidor!");
        } catch (UnknownHostException e) {
            System.err.println("Host não encontrado: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Erro ao conectar: " + e.getMessage());
        }
    }

    public void enviarMensagem(String mensagem) {
        if (saida == null) {
            System.err.println("Cliente não está conectado. Chame conectar() primeiro.");
            return;
        }
        saida.println(mensagem);
    }

    public void fechar() {
        try {
            if (saida != null) saida.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Erro ao fechar o cliente: " + e.getMessage());
        }
    }

    public boolean estaConectado() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }
}