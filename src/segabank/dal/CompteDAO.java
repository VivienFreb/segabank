package segabank.dal;

import segabank.bo.*;

import java.io.IOException;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class CompteDAO implements IDAO<Integer, Compte> {
    private static final String INSERT = "INSERT INTO compte (IdAgence, Type, Solde, Decouvert, TauxInteret, DateCreation) VALUES (?, ?, ?, ?, ?, ?);";
    private static final String UPDATE = "UPDATE compte SET IdAgence = ?, Type = ?, Solde = ?, Decouvert = ?, TauxInteret = ?, DateCreation = ? WHERE compte.Id = ?";
    private static final String DELETE = "DELETE FROM compte WHERE compte.Id = ?";
    private static final String QUERY_ALl = "SELECT Id, IdAgence, Type, Solde, Decouvert, TauxInteret, DateCreation FROM compte;";
    private static final String QUERY_ID = "SELECT Id, IdAgence, Type, Solde, Decouvert, TauxInteret, DateCreation FROM compte WHERE Id = ?";

    private static final String UPDATE_AFTER_CREATE = "SELECT DateCreation FROM compte WHERE Id = ?";

    private void buildPreparedStatement(PreparedStatement ps, Compte compte) throws SQLException {
        Compte.Etat type = compte.getType();
        ps.setInt(1, compte.getIdAgence());
        ps.setString(2, type.getLabel());
        ps.setDouble(3, compte.getSolde());
        ps.setString(4, null);
        ps.setString(5, null);
        ps.setTimestamp(6, new Timestamp(compte.getDateCreation().getTime()));
        switch (type) {
            case simple:
                ps.setDouble(4, ((CompteSimple)compte).getDecouvert());
                break;
            case epargne:
                ps.setDouble(5, ((CompteEpargne)compte).getTauxInteret());
        }
    }

    private Compte buildCompte(ResultSet rs) throws SQLException {
        Compte compte = null;
        Compte.Etat etat = Compte.Etat.valueOf(rs.getString("Type"));
        Integer id = rs.getInt("Id");
        Integer idAgence = rs.getInt("idAgence");
        Double solde = rs.getDouble("Solde");
        Date dateCreation = rs.getDate("DateCreation");

        switch (etat) {
            case epargne:
                CompteEpargne cptE = new CompteEpargne(id, solde, dateCreation, idAgence, rs.getDouble("TauxInteret"));
                compte = cptE;
                break;
            case simple:
                CompteSimple cptS = new CompteSimple(id, solde, dateCreation, idAgence, rs.getDouble("Decouvert"));
                compte = cptS;
                break;
            case payant:
                ComptePayant cptP = new ComptePayant(id, solde, dateCreation, idAgence);
                compte = cptP;
        }
        return compte;
    }

    @Override
    public void create(Compte compte) throws SQLException, IOException, ClassNotFoundException {
        Connection connection = PersistenceManager.getConnection();
        if (connection != null) {
            try (PreparedStatement ps = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
                buildPreparedStatement(ps, compte);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        compte.setId(rs.getInt(1));
                    }
                }
            }
            setUpdateDate(compte);
        }
    }

    private void setUpdateDate(Compte compte) throws SQLException, IOException, ClassNotFoundException {
        Connection connection = PersistenceManager.getConnection();
        if (connection != null) {
            try (PreparedStatement ps = connection.prepareStatement(UPDATE_AFTER_CREATE)) {
                ps.setInt(1, compte.getId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        compte.setDateCreation(rs.getDate(1));
                    }
                }
            }
        }
    }

    @Override
    public void update(Compte compte) throws SQLException, IOException, ClassNotFoundException {
        Connection connection = PersistenceManager.getConnection();
        if (connection != null) {
            try (PreparedStatement ps = connection.prepareStatement(UPDATE)) {
                buildPreparedStatement(ps, compte);
                ps.setInt(7, compte.getId());
                ps.executeUpdate();
            }
            setUpdateDate(compte);
        }
    }

    @Override
    public void delete(Compte compte) throws SQLException, IOException, ClassNotFoundException {
        Connection connection = PersistenceManager.getConnection();
        if (connection != null) {
            try (PreparedStatement ps = connection.prepareStatement(DELETE)) {
                ps.setInt(1, compte.getId());
                ps.executeUpdate();
            }
        }
    }

    @Override
    public Compte findById(Integer id) throws SQLException, IOException, ClassNotFoundException {
        Connection connection = PersistenceManager.getConnection();
        Compte compte = null;
        if (connection != null) {
            try (PreparedStatement ps = connection.prepareStatement(QUERY_ID)) {
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    compte = buildCompte(rs);
                }
                rs.close();
            }
        }
        return compte;
    }

    @Override
    public List<Compte> findAll() throws SQLException, IOException, ClassNotFoundException {
        Connection connection = PersistenceManager.getConnection();
        List<Compte> comptes = new ArrayList<>();
        if (connection != null) {
            try (PreparedStatement ps = connection.prepareStatement(QUERY_ALl)) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    comptes.add(buildCompte(rs));
                }
                rs.close();
            }
        }
        return comptes;
    }

    public List<Compte> buildFull() throws SQLException, IOException, ClassNotFoundException {
        List<Compte> comptes = findAll();
        OperationDAO operationDAO = new OperationDAO();
        List<Operation> operations = operationDAO.findAll();
        fillRelations(comptes, operations);
        Compte.setComptes(comptes);
        return comptes;
    }

    public void fillRelations(List<Compte> comptes, List<Operation> operations) {
        for (Compte compte : comptes) {
            for (Operation operation : operations) {
                if (operation.getIdCompte() == compte.getId()) {
                    operation.setCompte(compte);
                    compte.getOperations().add(operation);
                }
            }
        }
    }

    public void updateRelations(List<Compte> comptes) {
        List<Operation> operations = new ArrayList<>();
        for (Compte compte : comptes) {
            operations.addAll(compte.getOperations());
        }

        for (Compte compte : comptes) {
            for (Operation operation : operations) {
                if (operation.getIdCompte() != operation.getCompte().getId() && compte.getId() == operation.getIdCompte()) {
                    operation.getCompte().getOperations().remove(operation);
                    compte.getOperations().add(operation);
                    operation.setCompte(compte);
                }
            }
        }
    }
}
