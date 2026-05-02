package org.example.services;

import org.example.models.Livraison;
import org.example.utils.MyDatabase;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ServiceLivraison {

	public static class OptionItem {
		private final int id;
		private final String label;

		public OptionItem(int id, String label) {
			this.id = id;
			this.label = label;
		}

		public int getId() {
			return id;
		}

		@Override
		public String toString() {
			return label;
		}
	}

	private static final Set<String> STATUTS_AUTORISES = Set.of(
			"en_attente", "en_preparation", "en_cours", "livree", "retournee", "annulee"
	);

	private final Connection connection;
	private final boolean hasDateLivraisonColumn;
	private final boolean hasAddressLivraisonColumn;
	private final boolean hasStatutLivraisonColumn;

	public ServiceLivraison() {
		this.connection = MyDatabase.getInstance().getConnection();
		boolean dateLivraisonColumnExists = false;
		boolean addressLivraisonColumnExists = false;
		boolean statutLivraisonColumnExists = false;
		try {
			ensureSchema();
			dateLivraisonColumnExists = hasColumn("livraison", "datelivraison");
			addressLivraisonColumnExists = hasColumn("livraison", "addresslivraison");
			statutLivraisonColumnExists = hasColumn("livraison", "statutlivraison");
		} catch (SQLException e) {
			System.err.println("Erreur initialisation schema livraison: " + e.getMessage());
		}
		this.hasDateLivraisonColumn = dateLivraisonColumnExists;
		this.hasAddressLivraisonColumn = addressLivraisonColumnExists;
		this.hasStatutLivraisonColumn = statutLivraisonColumnExists;
	}

	private void ensureSchema() throws SQLException {
		try (Statement st = connection.createStatement()) {
			st.execute("CREATE TABLE IF NOT EXISTS livraison ("
					+ "id INT AUTO_INCREMENT PRIMARY KEY,"
					+ "commande_id INT NOT NULL,"
					+ "client_id INT NOT NULL,"
					+ "livreur_id INT NULL,"
					+ "adresse_livraison VARCHAR(255) NOT NULL,"
					+ "statut VARCHAR(32) NOT NULL DEFAULT 'en_attente',"
					+ "tracking_code VARCHAR(64) NOT NULL,"
					+ "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
					+ "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
					+ ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

			st.execute("ALTER TABLE livraison ADD COLUMN IF NOT EXISTS commande_id INT NOT NULL");
			st.execute("ALTER TABLE livraison ADD COLUMN IF NOT EXISTS client_id INT NOT NULL");
			st.execute("ALTER TABLE livraison ADD COLUMN IF NOT EXISTS livreur_id INT NULL");
			st.execute("ALTER TABLE livraison ADD COLUMN IF NOT EXISTS adresse_livraison VARCHAR(255) NOT NULL");
			st.execute("ALTER TABLE livraison ADD COLUMN IF NOT EXISTS statut VARCHAR(32) NOT NULL DEFAULT 'en_attente'");
			st.execute("ALTER TABLE livraison ADD COLUMN IF NOT EXISTS tracking_code VARCHAR(64) NOT NULL");
			st.execute("ALTER TABLE livraison ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
			st.execute("ALTER TABLE livraison ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
			st.execute("ALTER TABLE livraison ADD COLUMN IF NOT EXISTS datelivraison DATE NULL");

			st.execute("CREATE INDEX IF NOT EXISTS idx_livraison_client ON livraison(client_id)");
			st.execute("CREATE INDEX IF NOT EXISTS idx_livraison_livreur ON livraison(livreur_id)");
			st.execute("CREATE INDEX IF NOT EXISTS idx_livraison_statut ON livraison(statut)");
			st.execute("CREATE INDEX IF NOT EXISTS idx_livraison_created_at ON livraison(created_at)");
		}
	}

	public void ajouter(Livraison livraison) throws SQLException {
		validerLivraison(livraison, false);
		String adresse = livraison.getAdresseLivraison() == null ? "-" : livraison.getAdresseLivraison().trim();
		StringBuilder columns = new StringBuilder("commande_id, client_id, livreur_id");
		StringBuilder values = new StringBuilder("?, ?, ?");
		if (hasDateLivraisonColumn) {
			columns.append(", datelivraison");
			values.append(", ?");
		}
		columns.append(", adresse_livraison");
		values.append(", ?");
		if (hasAddressLivraisonColumn) {
			columns.append(", addresslivraison");
			values.append(", ?");
		}
		columns.append(", statut, tracking_code");
		values.append(", ?, ?");
		if (hasStatutLivraisonColumn) {
			columns.append(", statutlivraison");
			values.append(", ?");
		}
		String sql = "INSERT INTO livraison (" + columns + ") VALUES (" + values + ")";
		try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			int idx = 1;
			ps.setInt(idx++, livraison.getCommandeId());
			ps.setInt(idx++, livraison.getClientId());
			if (livraison.getLivreurId() == null || livraison.getLivreurId() <= 0) {
				ps.setNull(idx++, java.sql.Types.INTEGER);
			} else {
				ps.setInt(idx++, livraison.getLivreurId());
			}
			if (hasDateLivraisonColumn) {
				ps.setDate(idx++, java.sql.Date.valueOf(livraison.getDateLivraison() == null ? LocalDate.now() : livraison.getDateLivraison()));
			}
			ps.setString(idx++, adresse);
			if (hasAddressLivraisonColumn) {
				ps.setString(idx++, adresse);
			}
			String statut = normaliserStatut(livraison.getStatut());
			ps.setString(idx++, statut);
			ps.setString(idx++, safeTracking(livraison.getTrackingCode()));
			if (hasStatutLivraisonColumn) {
				ps.setString(idx, statut);
			}
			ps.executeUpdate();

			try (ResultSet rs = ps.getGeneratedKeys()) {
				if (rs.next()) {
					livraison.setId(rs.getInt(1));
				}
			}
		}
	}

	public void modifier(Livraison livraison) throws SQLException {
		validerLivraison(livraison, true);
		String adresse = livraison.getAdresseLivraison() == null ? "-" : livraison.getAdresseLivraison().trim();
		StringBuilder set = new StringBuilder("commande_id=?, client_id=?, livreur_id=?");
		if (hasDateLivraisonColumn) {
			set.append(", datelivraison=?");
		}
		set.append(", adresse_livraison=?");
		if (hasAddressLivraisonColumn) {
			set.append(", addresslivraison=?");
		}
		set.append(", statut=?, tracking_code=?");
		if (hasStatutLivraisonColumn) {
			set.append(", statutlivraison=?");
		}
		set.append(" WHERE id=?");
		String sql = "UPDATE livraison SET " + set;
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			int idx = 1;
			ps.setInt(idx++, livraison.getCommandeId());
			ps.setInt(idx++, livraison.getClientId());
			if (livraison.getLivreurId() == null || livraison.getLivreurId() <= 0) {
				ps.setNull(idx++, java.sql.Types.INTEGER);
			} else {
				ps.setInt(idx++, livraison.getLivreurId());
			}
			if (hasDateLivraisonColumn) {
				ps.setDate(idx++, java.sql.Date.valueOf(livraison.getDateLivraison() == null ? LocalDate.now() : livraison.getDateLivraison()));
			}
			ps.setString(idx++, adresse);
			if (hasAddressLivraisonColumn) {
				ps.setString(idx++, adresse);
			}
			String statut = normaliserStatut(livraison.getStatut());
			ps.setString(idx++, statut);
			ps.setString(idx++, safeTracking(livraison.getTrackingCode()));
			if (hasStatutLivraisonColumn) {
				ps.setString(idx++, statut);
			}
			ps.setInt(idx, livraison.getId());
			ps.executeUpdate();
		}
	}

	private boolean hasColumn(String tableName, String columnName) throws SQLException {
		DatabaseMetaData metaData = connection.getMetaData();
		try (ResultSet rs = metaData.getColumns(connection.getCatalog(), null, tableName, columnName)) {
			if (rs.next()) {
				return true;
			}
		}
		try (ResultSet rs = metaData.getColumns(connection.getCatalog(), null, tableName.toUpperCase(), columnName.toUpperCase())) {
			return rs.next();
		}
	}

	public void supprimer(int id) throws SQLException {
		try (PreparedStatement ps = connection.prepareStatement("DELETE FROM livraison WHERE id = ?")) {
			ps.setInt(1, id);
			ps.executeUpdate();
		}
	}

	public void updateStatut(int id, String statut) throws SQLException {
		String statutNormalise = normaliserStatut(statut);
		String sql = hasStatutLivraisonColumn
				? "UPDATE livraison SET statut=?, statutlivraison=? WHERE id=?"
				: "UPDATE livraison SET statut=? WHERE id=?";
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setString(1, statutNormalise);
			if (hasStatutLivraisonColumn) {
				ps.setString(2, statutNormalise);
				ps.setInt(3, id);
			} else {
				ps.setInt(2, id);
			}
			ps.executeUpdate();
		}
	}

	public List<Livraison> afficherTout() throws SQLException {
		return queryLivraisons(baseSelect() + " ORDER BY l.created_at DESC, l.id DESC", null);
	}

	public List<Livraison> afficherParClient(int clientId) throws SQLException {
		return queryLivraisons(baseSelect() + " WHERE l.client_id = ? ORDER BY l.created_at DESC, l.id DESC",
				ps -> ps.setInt(1, clientId));
	}

	public List<Livraison> afficherParLivreur(int livreurId) throws SQLException {
		return queryLivraisons(baseSelect() + " WHERE l.livreur_id = ? ORDER BY l.created_at DESC, l.id DESC",
				ps -> ps.setInt(1, livreurId));
	}

	public List<Livraison> afficherPourArtisan(int artisanId) throws SQLException {
		String sql = baseSelect()
				+ " JOIN commande_article ca ON ca.commande_id = l.commande_id"
				+ " JOIN article a ON a.id = ca.article_id"
				+ " WHERE a.artisan_id = ?"
				+ " ORDER BY l.created_at DESC, l.id DESC";
		return queryLivraisons(sql, ps -> ps.setInt(1, artisanId));
	}

	public List<String[]> getStatsByStatut(List<Livraison> livraisons) {
		int total = livraisons == null ? 0 : livraisons.size();
		int enAttente = 0;
		int enCours = 0;
		int livree = 0;

		if (livraisons != null) {
			for (Livraison l : livraisons) {
				String statut = l.getStatut();
				if ("en_attente".equals(statut) || "en_preparation".equals(statut)) {
					enAttente++;
				} else if ("en_cours".equals(statut)) {
					enCours++;
				} else if ("livree".equals(statut)) {
					livree++;
				}
			}
		}

		List<String[]> stats = new ArrayList<>();
		stats.add(new String[]{"total", String.valueOf(total)});
		stats.add(new String[]{"en_attente", String.valueOf(enAttente)});
		stats.add(new String[]{"en_cours", String.valueOf(enCours)});
		stats.add(new String[]{"livree", String.valueOf(livree)});
		return stats;
	}

	public Livraison trouverParId(int id) throws SQLException {
		List<Livraison> result = queryLivraisons(baseSelect() + " WHERE l.id = ?", ps -> ps.setInt(1, id));
		return result.isEmpty() ? null : result.get(0);
	}

	public List<Livraison> rechercher(String motCle) throws SQLException {
		String valeur = motCle == null ? "" : motCle.trim();
		if (valeur.isBlank()) {
			return afficherTout();
		}

		String pattern = "%" + valeur + "%";
		String sql = baseSelect()
				+ " WHERE l.tracking_code LIKE ? OR l.adresse_livraison LIKE ? OR c.numero LIKE ? OR l.statut LIKE ?"
				+ " ORDER BY l.created_at DESC, l.id DESC";
		return queryLivraisons(sql, ps -> {
			ps.setString(1, pattern);
			ps.setString(2, pattern);
			ps.setString(3, pattern);
			ps.setString(4, pattern);
		});
	}

	public List<OptionItem> getCommandesDisponibles() throws SQLException {
		String sql = "SELECT id, numero, client_id, adresselivraison FROM commande ORDER BY datecommande DESC LIMIT 300";
		List<OptionItem> list = new ArrayList<>();
		try (PreparedStatement ps = connection.prepareStatement(sql);
			 ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				int id = rs.getInt("id");
				String numero = rs.getString("numero");
				int clientId = rs.getInt("client_id");
				String adresse = rs.getString("adresselivraison");
				String label = "#" + id + " - " + (numero == null ? "CMD" : numero)
						+ " - Client " + clientId + " - " + (adresse == null ? "adresse" : adresse);
				list.add(new OptionItem(id, label));
			}
		}
		return list;
	}

	public List<OptionItem> getLivreurs() throws SQLException {
		String sql = "SELECT id, CONCAT(COALESCE(prenom,''), ' ', COALESCE(nom,'')) AS nomComplet FROM `user` "
				+ "WHERE role='LIVREUR' AND (deleted_at IS NULL OR deleted_at = '') ORDER BY id DESC";
		List<OptionItem> list = new ArrayList<>();
		try (PreparedStatement ps = connection.prepareStatement(sql);
			 ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				int id = rs.getInt("id");
				String nom = rs.getString("nomComplet");
				String label = "#" + id + " - " + (nom == null || nom.isBlank() ? "Livreur" : nom.trim());
				list.add(new OptionItem(id, label));
			}
		}
		return list;
	}

	public Integer findClientIdByCommande(int commandeId) throws SQLException {
		String sql = "SELECT client_id FROM commande WHERE id = ?";
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setInt(1, commandeId);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					Object value = rs.getObject("client_id");
					return value == null ? null : ((Number) value).intValue();
				}
			}
		}
		return null;
	}


	private String baseSelect() {
		return "SELECT l.*, c.numero AS commande_numero FROM livraison l LEFT JOIN commande c ON c.id = l.commande_id";
	}

	private List<Livraison> queryLivraisons(String sql, SqlConsumer consumer) throws SQLException {
		List<Livraison> livraisons = new ArrayList<>();
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			if (consumer != null) {
				consumer.accept(ps);
			}
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					livraisons.add(map(rs));
				}
			}
		}
		return livraisons;
	}

	private Livraison map(ResultSet rs) throws SQLException {
		Livraison l = new Livraison();
		l.setId(rs.getInt("id"));
		l.setCommandeId(rs.getInt("commande_id"));
		l.setNumeroCommande(rs.getString("commande_numero"));
		l.setClientId(rs.getInt("client_id"));

		Object livreur = rs.getObject("livreur_id");
		l.setLivreurId(livreur == null ? null : ((Number) livreur).intValue());

		l.setAdresseLivraison(rs.getString("adresse_livraison"));
		String statut = rs.getString("statut");
		if ((statut == null || statut.isBlank()) && hasStatutLivraisonColumn) {
			try {
				statut = rs.getString("statutlivraison");
			} catch (SQLException ignored) {
				// Compatibilite schema: on garde la valeur statut standard si disponible.
			}
		}
		l.setStatut(statut);
		l.setTrackingCode(rs.getString("tracking_code"));
		l.setCreatedAt(rs.getTimestamp("created_at"));
		l.setUpdatedAt(rs.getTimestamp("updated_at"));
		return l;
	}

	private void validerLivraison(Livraison livraison, boolean requireId) {
		if (livraison == null) {
			throw new IllegalArgumentException("Livraison invalide.");
		}
		if (requireId && livraison.getId() <= 0) {
			throw new IllegalArgumentException("Identifiant livraison invalide.");
		}
		if (livraison.getCommandeId() <= 0) {
			throw new IllegalArgumentException("Commande invalide.");
		}
		if (livraison.getClientId() <= 0) {
			throw new IllegalArgumentException("Client invalide.");
		}
		String adresse = livraison.getAdresseLivraison() == null ? "" : livraison.getAdresseLivraison().trim();
		// Adresse permissive pour autoriser la creation, la carte gerera ensuite le positionnement.
		if (adresse.isBlank()) {
			livraison.setAdresseLivraison("-");
		}
		normaliserStatut(livraison.getStatut());
	}

	private String normaliserStatut(String statut) {
		String value = statut == null ? "en_attente" : statut.trim().toLowerCase();
		if (value.isBlank()) {
			value = "en_attente";
		}
		if (!STATUTS_AUTORISES.contains(value)) {
			throw new IllegalArgumentException("Statut livraison invalide.");
		}
		return value;
	}

	private String safeTracking(String tracking) {
		if (tracking == null || tracking.isBlank()) {
			return "LIV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
		}
		return tracking.trim();
	}

	@FunctionalInterface
	private interface SqlConsumer {
		void accept(PreparedStatement statement) throws SQLException;
	}
}

