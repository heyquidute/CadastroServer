import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Servidor {

    public static void main(String[] args) {
        int porta = 12345; // Porta onde o servidor vai ouvir as conexões

        try (ServerSocket servidorSocket = new ServerSocket(porta)) {
            System.out.println("Servidor iniciado e ouvindo na porta " + porta);

            while (true) {
                // Aceitar uma nova conexão de cliente
                Socket clienteSocket = servidorSocket.accept();
                System.out.println("Cliente conectado: " + clienteSocket.getInetAddress().getHostAddress());

                // Cria uma nova thread para lidar com o cliente
                new Thread(new ClienteHandler(clienteSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("Erro ao iniciar o servidor: " + e.getMessage());
        }
    }
}

class ClienteHandler implements Runnable {
    private Socket clienteSocket;

    public ClienteHandler(Socket socket) {
        this.clienteSocket = socket;
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clienteSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clienteSocket.getOutputStream(), true);
        ) {
            //Receber login
            out.println("Digite o login:");
            String login = in.readLine();

            //Receber senha
            out.println("Digite o senha:");
            String senha = in.readLine();

            if (validarCredenciais(login, senha)){
                out.println("Login bem sucedido!");
                System.out.printf("Cliente autenticado" + clienteSocket.getInetAddress());

                //Loop de comunicação com o cliente
                String mensagem;
                while ((mensagem = in.readLine()) != null){
                    if (mensagem.equalsIgnoreCase("L")){
                        enviarListaDeProdutos(out);
                    } else {
                        out.println("Comando não reconhecido. Tente novamente!");
                    }
                }
            } else {
                out.println("Credenciais incorretas! Conexão encerrada.");
                System.out.printf("Cliente desconectado por falta na autenticação" + clienteSocket.getInetAddress());
                clienteSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Erro ao comunicar com o cliente: " + e.getMessage());
        } finally {
            try {
                if (!clienteSocket.isClosed()){
                    clienteSocket.close();
                }
            } catch (IOException e) {
                System.err.println("Erro ao fechar o socket do cliente: " + e.getMessage());
            }
        }
    }
    private boolean validarCredenciais(String login, String senha) {
        if (login.equals("loja") && senha.equals("loja")){
            return true;
        } else {
            return false;
        }
    }

    private void enviarListaDeProdutos(PrintWriter out) {
        try(Connection conexao = ConexaoBanco.getConexao();
            Statement statement = conexao.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM Produtos")){

            out.println("Lista de Produtos:");
            while(resultSet.next()){
                int id = resultSet.getInt("id_produto");
                String nome = resultSet.getString("nome_produto");
                double preco = resultSet.getDouble("valor_produto");

                String produtoInfo = String.format("ID: %d; Nome: %s; Preço: R$%.2f", id, nome, preco);
                out.println(produtoInfo);
            }
        } catch (SQLException e) {
            out.println("Erro ao enviar Lista de Produtos: " + e.getMessage());
            System.err.println("Erro ao enviar Lista de Produtos: " + e.getMessage());
        }

    }
}
