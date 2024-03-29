package segabank;

import segabank.bo.*;
import segabank.dal.AgenceDAO;
import segabank.menu.Menu;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class App {
    public static void main(String[] args) {

        AgenceDAO agenceDAO = new AgenceDAO();
        List<Agence> agences = null;

        try {
            agences = agenceDAO.buildFull();
        } catch (SQLException | IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        if (agences == null) {
            System.out.println("ERROR");
            System.exit(0);
        }

        Menu menu = new Menu(agences);
        menu.printMenu();
    }
}
