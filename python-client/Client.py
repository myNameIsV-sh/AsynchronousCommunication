import socket

class Client:
    def __init__(self, host, porta):
        self.host = host
        self.porta = porta
        self.socket = None

    def conectar(self):
        """Estabelece a conexão com o servidor."""
        try:
            self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.socket.connect((self.host, self.porta))
            self.reader = self.socket.makefile('r', encoding='utf-8')
            print("O cliente se conectou ao servidor!")
        except ConnectionRefusedError:
            print("Erro ao conectar: Conexão recusada (servidor offline).")
        except Exception as e:
            print(f"Erro inesperado ao conectar: {e}")

    def enviar_mensagem(self, mensagem):
        """Envia uma mensagem para o servidor."""
        if not self.esta_conectado():
            print("Cliente não está conectado. Chame conectar() primeiro.")
            return False
        
        try:
            self.socket.sendall((mensagem + "\n").encode('utf-8'))
            return True
        except (ConnectionResetError, BrokenPipeError):
            print("Erro ao enviar: O servidor encerrou a conexão.")
            return False

    def receber_resposta(self, buffer_size=1024):
        """Aguarda e recebe uma resposta do servidor."""
        if not self.esta_conectado():
            return None
        
        try:
            linha = self.reader.readline()
            if not linha:
                print("O servidor encerrou a sessão.")
                return None
            return linha.strip() 
        except Exception as e:
            print(f"Erro ao receber dados: {e}")
            return None

    def fechar(self):
        """Fecha a conexão com o servidor."""
        if self.socket:
            self.socket.close()
            self.socket = None
            print("Conexão encerrada.")

    def esta_conectado(self):
        """Verifica se o socket ainda é válido."""
        return self.socket is not None