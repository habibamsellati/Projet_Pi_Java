package org.example.models;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Commande {
	private static final String PREFIXE_NUMERO = "CMD";
	private static final String CARACTERES = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	private static final SecureRandom RANDOM = new SecureRandom();

	private int id;
	private String numero;
	private Timestamp dateCommande;
	private String statut;
	private Double total;
	private String adresseLivraison;
	private String telephone;
	private String modePaiement;
	private Integer clientId;
	private List<Article> articles = new ArrayList<>();
	/** Nom complet du client (renseigné par requêtes JOIN, non persisté en base). */
	private String nomClient;
	/** Email du client connecté (non persisté en base, utilisé pour l'envoi email). */
	private String emailClient;
	private String messagePersonnalise;
	private boolean aiGenerated;

	public Commande() {
	}

	public Commande(String adresseLivraison, String telephone, String modePaiement, String statut, Double total) {
		this.adresseLivraison = adresseLivraison;
		this.telephone = telephone;
		this.modePaiement = modePaiement;
		this.statut = statut;
		this.total = total;
		this.numero = generateNumero();
	}

	public double calculateTotal() {
		double somme = 0;
		for (Article article : articles) {
			if (article != null && article.getPrix() != null) {
				somme += article.getPrix();
			}
		}
		this.total = somme;
		return somme;
	}

	public boolean canBeCancelled() {
		return "en_attente".equals(statut) || "confirmee".equals(statut);
	}

	public static String generateNumero() {
		String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
		StringBuilder suffixe = new StringBuilder();
		for (int i = 0; i < 6; i++) {
			suffixe.append(CARACTERES.charAt(RANDOM.nextInt(CARACTERES.length())));
		}
		return PREFIXE_NUMERO + "-" + date + "-" + suffixe;
	}

	public int getId() { return id; }
	public void setId(int id) { this.id = id; }
	public String getNumero() { return numero; }
	public void setNumero(String numero) { this.numero = numero; }
	public Timestamp getDateCommande() { return dateCommande; }
	public void setDateCommande(Timestamp dateCommande) { this.dateCommande = dateCommande; }
	public String getStatut() { return statut; }
	public void setStatut(String statut) { this.statut = statut; }
	public Double getTotal() { return total; }
	public void setTotal(Double total) { this.total = total; }
	public String getAdresseLivraison() { return adresseLivraison; }
	public void setAdresseLivraison(String adresseLivraison) { this.adresseLivraison = adresseLivraison; }
	public String getTelephone() { return telephone; }
	public void setTelephone(String telephone) { this.telephone = telephone; }
	public String getModePaiement() { return modePaiement; }
	public void setModePaiement(String modePaiement) { this.modePaiement = modePaiement; }
	public Integer getClientId() { return clientId; }
	public void setClientId(Integer clientId) { this.clientId = clientId; }
	public List<Article> getArticles() { return articles; }
	public void setArticles(List<Article> articles) { this.articles = articles == null ? new ArrayList<>() : articles; }
	public void addArticle(Article article) {
		if (article != null) {
			this.articles.add(article);
		}
	}
	public String getNomClient() { return nomClient; }
	public void setNomClient(String nomClient) { this.nomClient = nomClient; }
	public String getEmailClient() { return emailClient; }
	public void setEmailClient(String emailClient) { this.emailClient = emailClient; }
	public String getMessagePersonnalise() { return messagePersonnalise; }
	public void setMessagePersonnalise(String messagePersonnalise) { this.messagePersonnalise = messagePersonnalise; }
	public boolean isAiGenerated() { return aiGenerated; }
	public void setAiGenerated(boolean aiGenerated) { this.aiGenerated = aiGenerated; }
}
