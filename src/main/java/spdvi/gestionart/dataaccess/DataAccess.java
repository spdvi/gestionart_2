package spdvi.gestionart.dataaccess;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import spdvi.gestionart.Constants;
import spdvi.gestionart.Models.Espai;
import spdvi.gestionart.Models.Municipi;
import spdvi.gestionart.Models.Usuari;
import spdvi.gestionart.Utils.PasswordHasher;
import spdvi.gestionart.models.Comentari;
import spdvi.gestionart.utils.ImageUtils;
import static spdvi.gestionart.utils.ImageUtils.readAllBytes;

/**
 *
 * @author DevMike
 */
public class DataAccess {

    private Connection getConnection() {
        Connection connection = null;
        Properties properties = new Properties();
        try {
            properties.load(DataAccess.class.getClassLoader().getResourceAsStream("properties/application.properties"));
            connection = DriverManager.getConnection(properties.getProperty("url"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return connection;
    }

    public String findUserPassword(String email) {
        String passwordInDb = null;
        String sql = "SELECT password FROM dbo.usuaris WHERE email = ?";
        try (Connection connection = getConnection();
                PreparedStatement selectStatement = connection.prepareStatement(sql);) {
            selectStatement.setString(1, email);
            ResultSet resultSet = selectStatement.executeQuery();
            while (resultSet.next()) {
                passwordInDb = resultSet.getString("password");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return passwordInDb;
    }

    public int getUserId(String email) {
        int userId = 0;
        String sql = "SELECT id FROM dbo.usuaris WHERE email = ?";
        try (Connection connection = getConnection();
                PreparedStatement selectStatement = connection.prepareStatement(sql);) {
            selectStatement.setString(1, email);
            ResultSet resultSet = selectStatement.executeQuery();
            while (resultSet.next()) {
                userId = resultSet.getInt("id");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return userId;
    }

    public Usuari getUser(String email) {
        Usuari user = null;
        String sql = "SELECT * FROM dbo.usuaris WHERE email = ?";
        try (Connection connection = getConnection();
                PreparedStatement selectStatement = connection.prepareStatement(sql);) {
            selectStatement.setString(1, email);
            ResultSet resultSet = selectStatement.executeQuery();
            user = new Usuari();
            while (resultSet.next()) {
                user.setId(resultSet.getInt("id"));
                user.setNom(resultSet.getString("nom"));
                user.setLlinatges(resultSet.getString("llinatges"));
                user.setDni(resultSet.getString("dni"));
                user.setTelefon(resultSet.getString("telefon"));
                user.setEmail(resultSet.getString("email"));
                user.setPassword(resultSet.getString("password"));
                user.setPasswordHash(resultSet.getBytes("password_hash"));
                InputStream profilePictureIS = resultSet.getBinaryStream("profilePicture");
                if (profilePictureIS != null) {
                    user.setProfilePicture(readAllBytes(profilePictureIS));
                } else {
                    user.setProfilePicture(null);
                }
                user.setAdmin(resultSet.getBoolean("isAdmin"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IOException ex) {
            Logger.getLogger(DataAccess.class.getName()).log(Level.SEVERE, null, ex);
        }
        return user;
    }

    public int insertUser(Usuari user) {
        int result = 0;
        String sql = "INSERT INTO dbo.usuaris (nom, llinatges, email, password, password_hash) VALUES (?,?,?,?,?)";
        try (Connection connection = getConnection();
                PreparedStatement insertStatement = connection.prepareStatement(sql);) {
            insertStatement.setString(1, user.getNom());
            insertStatement.setString(2, user.getLlinatges());
            insertStatement.setString(3, user.getEmail());
            insertStatement.setString(4, user.getPassword());
            insertStatement.setBytes(5, user.getPasswordHash());
            result = insertStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    public UUID insertSession(int userId) {
        UUID uuid = null;
        String sql = "INSERT INTO dbo.[session] (uuid, id_usuari, ip_address) VALUES (?,?,?)";
        try (Connection connection = getConnection();
                PreparedStatement insertStatement = connection.prepareStatement(sql);) {
            uuid = UUID.randomUUID();
            insertStatement.setString(1, uuid.toString());
            insertStatement.setInt(2, userId);
            InetAddress ip = InetAddress.getLocalHost();
            insertStatement.setBytes(3, ip.getAddress());
            insertStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (UnknownHostException ex) {
            Logger.getLogger(DataAccess.class.getName()).log(Level.SEVERE, null, ex);
        }
        return uuid;
    }

    public int deleteSession(UUID uuid) {
        int result = 0;
        String sql = "DELETE FROM dbo.[session] WHERE uuid = ?";
        try (Connection connection = getConnection();
                PreparedStatement deleteStatement = connection.prepareStatement(sql);) {
            deleteStatement.setString(1, uuid.toString());
            result = deleteStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    public ArrayList<Municipi> getMunicipis() {
        ArrayList<Municipi> municipis = new ArrayList<>();
        String sql = "SELECT * FROM dbo.municipis";
        try (Connection connection = getConnection();
                PreparedStatement selectStatement = connection.prepareStatement(sql);) {
            ResultSet resultSet = selectStatement.executeQuery();
            while (resultSet.next()) {
                Municipi municipi = new Municipi();
                municipi.setId(resultSet.getInt("id"));
                municipi.setNom(resultSet.getString("nom"));
                municipi.setIlla(resultSet.getString("illa"));
                municipis.add(municipi);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return municipis;
    }

    public int getEspaisCount(String searchString) {
        int count = 0;
        String sql = "SELECT COUNT(*) AS count_espais"
                + " FROM dbo.espais JOIN dbo.municipis ON dbo.espais.municipi=dbo.municipis.id JOIN dbo.tipus_espai ON dbo.espais.tipus=dbo.tipus_espai.id"
                + " WHERE visible=1 AND espais.nom LIKE CONCAT('%',?,'%')";
        try (Connection connection = getConnection();
                PreparedStatement selectStatement = connection.prepareStatement(sql);) {
            selectStatement.setString(1, searchString);
            ResultSet resultSet = selectStatement.executeQuery();
            while (resultSet.next()) {
                count = resultSet.getInt("count_espais");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return count;
    }

    public ArrayList<Espai> getEspais(String searchString, int offset) {
        ArrayList<Espai> espais = new ArrayList<>();
        String sql = "SELECT espais.nom, registre, espais.descripcio, municipis.nom AS nom_municipi, adreca, email, web, telefon, tipus_espai.descripcio AS descripcio_espai"
                + " FROM dbo.espais JOIN dbo.municipis ON dbo.espais.municipi=dbo.municipis.id JOIN dbo.tipus_espai ON dbo.espais.tipus=dbo.tipus_espai.id"
                + " WHERE visible=1 AND espais.nom LIKE CONCAT('%',?,'%')"
                + " ORDER BY registre"
                + " OFFSET ? ROWS"
                + " FETCH NEXT 10 ROWS ONLY";
        try (Connection connection = getConnection();
                PreparedStatement selectStatement = connection.prepareStatement(sql);) {
            selectStatement.setString(1, searchString);
            selectStatement.setInt(2, offset * 10);
            ResultSet resultSet = selectStatement.executeQuery();
            while (resultSet.next()) {
                Espai espai = new Espai();
                espai.setNom(resultSet.getString("nom"));
                espai.setRegistre(resultSet.getString("registre"));
                espai.setDescripcio(resultSet.getString("descripcio"));
                espai.setMunicipi(resultSet.getString("nom_municipi"));
                espai.setAdreca(resultSet.getString("adreca"));
                espai.setEmail(resultSet.getString("email"));
                espai.setWeb(resultSet.getString("web"));
                espai.setTelefon(resultSet.getString("telefon"));
                espai.setTipus(resultSet.getString("descripcio_espai"));
                espais.add(espai);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return espais;
    }

    public ArrayList<String> getModalitatsEspai(String register) {
        ArrayList<String> modalitats = new ArrayList<>();
        String sql = "SELECT descripcio FROM dbo.modalitats JOIN dbo.modalitats_espai ON dbo.modalitats.id=dbo.modalitats_espai.id_modalitat"
                + " WHERE id_espai = ?";
        try (Connection connection = getConnection();
                PreparedStatement selectStatement = connection.prepareStatement(sql);) {
            selectStatement.setString(1, register);
            ResultSet resultSet = selectStatement.executeQuery();
            while (resultSet.next()) {
                modalitats.add(resultSet.getString("descripcio"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return modalitats;
    }

    public ArrayList<String> getServeisEspai(String registre) {
        ArrayList<String> serveis = new ArrayList<>();
        String sql = "SELECT descripcio FROM dbo.serveis JOIN dbo.serveis_espai ON dbo.serveis.id=dbo.serveis_espai.id_servei"
                + " WHERE id_espai = ?";
        try (Connection connection = getConnection();
                PreparedStatement selectStatement = connection.prepareStatement(sql);) {
            selectStatement.setString(1, registre);
            ResultSet resultSet = selectStatement.executeQuery();
            while (resultSet.next()) {
                serveis.add(resultSet.getString("descripcio"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return serveis;
    }

    public ArrayList<byte[]> getImatgesEspai(String registre) {
        ArrayList<byte[]> imatges = new ArrayList<>();
        String sql = "SELECT imatge FROM dbo.imatges JOIN dbo.imatges_espai ON dbo.imatges.id=dbo.imatges_espai.id_imatge"
                + " WHERE id_espai = ?";
        try (Connection connection = getConnection();
                PreparedStatement selectStatement = connection.prepareStatement(sql);) {
            selectStatement.setString(1, registre);
            ResultSet resultSet = selectStatement.executeQuery();
            while (resultSet.next()) {
                byte[] imatge = resultSet.getBytes(1);
                imatges.add(imatge);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return imatges;
    }

    public ArrayList<Comentari> getComentarisEspai(String registre) {
        ArrayList<Comentari> comentaris = new ArrayList<>();
        String sql = "SELECT text, dataihora, nom, llinatges, valoracio FROM dbo.comentaris JOIN dbo.usuaris ON dbo.comentaris.id_usuari=dbo.usuaris.id"
                + " WHERE id_espai = ?";
        try (Connection connection = getConnection();
                PreparedStatement selectStatement = connection.prepareStatement(sql);) {
            selectStatement.setString(1, registre);
            ResultSet resultSet = selectStatement.executeQuery();
            while (resultSet.next()) {
                String text = resultSet.getString("text");
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S");
                LocalDateTime dataihora = LocalDateTime.parse(resultSet.getString("dataihora"), dtf);
                String usuari = resultSet.getString("nom") + " " + resultSet.getString("llinatges");
                int valoracio = resultSet.getInt("valoracio");
                //String comentari = usuari + " said:\n " + text + "\nOn " + dataihora.toString() + "\nValoració: " + valoracio;
                Comentari comentari = new Comentari();
                comentari.setText(text);
                comentari.setDataihora(dataihora);
                comentari.setNom(usuari);
                comentari.setValoracio(valoracio);
                comentaris.add(comentari);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return comentaris;
    }

    public float getValoracioEspaiAvg(String registre) {
        float valoracioAvg = 0.0f;
        String sql = "SELECT AVG(valoracio) AS avg_valoracio FROM dbo.comentaris WHERE id_espai = ?";
        try (Connection connection = getConnection();
                PreparedStatement selectStatement = connection.prepareStatement(sql);) {
            selectStatement.setString(1, registre);
            ResultSet resultSet = selectStatement.executeQuery();
            while (resultSet.next()) {
                valoracioAvg = resultSet.getFloat("avg_valoracio");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return valoracioAvg;
    }

    public int insertComentariEspai(String text, int userId, String registre, int valoracio) {
        int result = 0;
        String sql = "INSERT INTO dbo.comentaris ([text], dataihora, id_usuari, id_espai, valoracio) VALUES (?,?,?,?,?)";
        try (Connection connection = getConnection();
                PreparedStatement insertStatement = connection.prepareStatement(sql);) {
            insertStatement.setString(1, text);
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S");
            String now = LocalDateTime.now().format(dtf);
            insertStatement.setString(2, now);
            insertStatement.setInt(3, userId);
            insertStatement.setString(4, registre);
            insertStatement.setInt(5, valoracio);
            result = insertStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    public Iterable<Espai> getEspaisByMunicipi(int idMunicipi) {
        ArrayList<Espai> espais = new ArrayList<>();
        String sql = "SELECT espais.nom, registre, espais.descripcio, municipis.nom AS nom_municipi, adreca, email, web, telefon, tipus_espai.descripcio AS descripcio_espai"
                + " FROM dbo.espais JOIN dbo.municipis ON dbo.espais.municipi=dbo.municipis.id JOIN dbo.tipus_espai ON dbo.espais.tipus=dbo.tipus_espai.id"
                + " WHERE visible=1 AND municipis.id=?";
        try (Connection connection = getConnection();
                PreparedStatement selectStatement = connection.prepareStatement(sql);) {
            selectStatement.setInt(1, idMunicipi);
            ResultSet resultSet = selectStatement.executeQuery();
            while (resultSet.next()) {
                Espai espai = new Espai();
                espai.setNom(resultSet.getString("nom"));
                espai.setRegistre(resultSet.getString("registre"));
                espai.setDescripcio(resultSet.getString("descripcio"));
                espai.setMunicipi(resultSet.getString("nom_municipi"));
                espai.setAdreca(resultSet.getString("adreca"));
                espai.setEmail(resultSet.getString("email"));
                espai.setWeb(resultSet.getString("web"));
                espai.setTelefon(resultSet.getString("telefon"));
                espai.setTipus(resultSet.getString("descripcio_espai"));
                espais.add(espai);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return espais;
    }

    public boolean changePassword(int userId, String password) {
        int result = 0;
        String sql = "UPDATE dbo.usuaris SET password = ?, password_hash = ? WHERE id = ?";
        try (Connection connection = getConnection();
                PreparedStatement updateStatement = connection.prepareStatement(sql);) {
            updateStatement.setString(1, password);
            updateStatement.setBytes(2, PasswordHasher.hashPassword(password));
            updateStatement.setInt(3, userId);
            result = updateStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result == 1 ? true : false;
    }

    public boolean deleteAccount(int userId) {
        // TODO: Rewrite this code to execute a transaction. Better in a stored procedure.
        int result = 0;
        String sql1 = "DELETE FROM dbo.comentaris WHERE id_usuari = ?";
        String sql2 = "DELETE FROM dbo.usuaris WHERE id = ?";
        try (Connection connection = getConnection();
                PreparedStatement deleteStatement = connection.prepareStatement(sql1);) {
            deleteStatement.setInt(1, userId);
            result = deleteStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try (Connection connection = getConnection();
                PreparedStatement deleteStatement = connection.prepareStatement(sql2);) {
            deleteStatement.setInt(1, userId);
            result = deleteStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result == 1 ? true : false;
    }

    public boolean updateUserProfilePicture(int userId, ImageIcon icon) {
        int result = 0;
        String sql = "UPDATE dbo.usuaris SET profilePicture = ? WHERE id = ?";
        try (Connection connection = getConnection();
                PreparedStatement updateStatement = connection.prepareStatement(sql);) {
            byte[] binaryImage = ImageUtils.toByteArray(icon.getImage());
            updateStatement.setBytes(1, binaryImage);
            updateStatement.setInt(2, userId);
            result = updateStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception ex) {
            Logger.getLogger(DataAccess.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result == 1 ? true : false;

    }

}
