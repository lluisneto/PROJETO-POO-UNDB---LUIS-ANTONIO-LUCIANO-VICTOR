/*
 * Descrição do MÓDULO do sistema (Java Platform Module System, desde o Java 9).
 *
 * Um módulo diz duas coisas:
 *  - "requires": de quais outros módulos o nosso código precisa;
 *  - "opens"/"exports": quais pacotes nossos outros módulos podem enxergar.
 * Tudo o que não está liberado aqui fica escondido: é o encapsulamento aplicado
 * ao projeto inteiro, não só a uma classe.
 */
module br.com.controleagil {

    // Botões, tabelas, campos de texto... e o próprio Application/Stage (traz o javafx.graphics junto).
    requires javafx.controls;

    // O FXMLLoader, que lê os arquivos .fxml da pasta resources/view e monta as telas.
    requires javafx.fxml;

    // JDBC (Connection, DriverManager...), usado pelos repositórios SQLite em repository.sqlite.
    // O driver do SQLite em si é encontrado sozinho pelo DriverManager, por isso não precisa de "requires".
    requires java.sql;

    // O FXMLLoader usa reflexão para criar os controllers e preencher os campos @FXML,
    // inclusive os privados. "opens" libera essa reflexão só para o javafx.fxml.
    opens br.com.controleagil.controller to javafx.fxml;

    // O JavaFX precisa criar a classe App (que estende Application) para abrir a janela.
    exports br.com.controleagil;
}
