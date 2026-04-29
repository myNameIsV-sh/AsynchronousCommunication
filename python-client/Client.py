import socket
import select

class Client:
    def __init__(self, host, porta):
        self.host = host
        self.porta = porta
        self.socket = None
        self._buffer = b""

    def conectar(self):
        """Estabelece a conexão com o servidor."""
        try:
            self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.socket.connect((self.host, self.porta))
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

    def _ler_uma_linha(self, timeout=None):
        """
        Lê do socket até encontrar uma linha completa ou dar timeout.
        Retorna a linha decodificada ou None se houver erro/timeout.
        """
        while b'\n' not in self._buffer:
            if timeout is not None:
                # Verifica se há dados disponíveis antes de ler
                ready, _, _ = select.select([self.socket], [], [], timeout)
                if not ready:
                    return None

            try:
                dados = self.socket.recv(4096)
                if not dados:
                    return None
                self._buffer += dados
            except Exception:
                return None

        # Extrai a linha do buffer
        pos = self._buffer.find(b'\n')
        linha_bytes = self._buffer[:pos]
        self._buffer = self._buffer[pos+1:]
        return linha_bytes.decode('utf-8').rstrip('\r\n')

    def receber_resposta(self):
        """
        Aguarda e recebe a resposta completa do servidor.
        """
        if not self.esta_conectado():
            return None

        linhas = []

        # 1. Lê a primeira linha (bloqueante — aguarda o servidor responder)
        primeira = self._ler_uma_linha(timeout=None)
        if primeira is None:
            # Se não há mais nada no buffer, a conexão provavelmente fechou
            if not self._buffer:
                print("O servidor encerrou a sessão.")
            return None
        linhas.append(primeira)

        # 2. Tenta ler linhas adicionais com timeout curto (200ms)
        while True:
            linha = self._ler_uma_linha(timeout=0.2)
            if linha is None:
                break
            linhas.append(linha)

        return "\n".join(linhas)

    def receber_linha(self):
        """
        Lê exatamente uma linha do servidor.
        """
        if not self.esta_conectado():
            return None
        return self._ler_uma_linha()

    def fechar(self):
        """Fecha a conexão com o servidor."""
        if self.socket:
            self.socket.close()
            self.socket = None
            self._buffer = b""
            print("Conexão encerrada.")

    def esta_conectado(self):
        """Verifica se o socket ainda é válido."""
        return self.socket is not None