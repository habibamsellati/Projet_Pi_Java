package org.example.models;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Commentaire {
	private int id;
	private String contenu;
	private Timestamp datePub;

	// Reactions
	private int likes = 0;
	private int dislikes = 0;

	// Relations
	private Article article;
	private User auteur;

	// Reponses imbriquees
	private Commentaire parent;
	private final List<Commentaire> replies = new ArrayList<>();

	public Commentaire() {
		this.datePub = new Timestamp(System.currentTimeMillis());
	}

	public Commentaire(String contenu, Article article, User auteur) {
		this();
		setContenu(contenu);
		this.article = article;
		this.auteur = auteur;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getContenu() {
		return contenu;
	}

	public void setContenu(String contenu) {
		String value = contenu == null ? "" : contenu.trim();
		int visibleLength = value.codePointCount(0, value.length());
		if (visibleLength < 5 || visibleLength > 255) {
			throw new IllegalArgumentException("Le contenu doit contenir entre 5 et 255 caractères.");
		}
		this.contenu = value;
	}

	public Timestamp getDatePub() {
		return datePub;
	}

	public void setDatePub(Timestamp datePub) {
		this.datePub = datePub == null ? new Timestamp(System.currentTimeMillis()) : datePub;
	}

	public int getLikes() {
		return likes;
	}

	public void setLikes(int likes) {
		this.likes = Math.max(0, likes);
	}

	public int getDislikes() {
		return dislikes;
	}

	public void setDislikes(int dislikes) {
		this.dislikes = Math.max(0, dislikes);
	}

	public void incrementLikes() {
		likes++;
	}

	public void decrementLikes() {
		if (likes > 0) {
			likes--;
		}
	}

	public void incrementDislikes() {
		dislikes++;
	}

	public void decrementDislikes() {
		if (dislikes > 0) {
			dislikes--;
		}
	}

	// Si l'utilisateur change d'avis: on retire l'ancienne reaction puis on applique la nouvelle.
	public void react(boolean likedBefore, boolean dislikedBefore, boolean likeNow, boolean dislikeNow) {
		if (likedBefore) {
			decrementLikes();
		}
		if (dislikedBefore) {
			decrementDislikes();
		}
		if (likeNow) {
			incrementLikes();
		}
		if (dislikeNow) {
			incrementDislikes();
		}
	}

	public Article getArticle() {
		return article;
	}

	public void setArticle(Article article) {
		this.article = article;
	}

	public User getAuteur() {
		return auteur;
	}

	public void setAuteur(User auteur) {
		this.auteur = auteur;
	}

	public Commentaire getParent() {
		return parent;
	}

	public void setParent(Commentaire parent) {
		this.parent = parent;
	}

	public List<Commentaire> getReplies() {
		return Collections.unmodifiableList(replies);
	}

	public void addReply(Commentaire reply) {
		if (reply == null || reply == this) {
			return;
		}
		reply.setParent(this);
		replies.add(reply);
	}

	// Simule orphanRemoval: suppression du parent + toutes ses reponses en memoire.
	public void clearRepliesCascade() {
		for (Commentaire reply : replies) {
			reply.clearRepliesCascade();
			reply.setParent(null);
		}
		replies.clear();
	}
}
