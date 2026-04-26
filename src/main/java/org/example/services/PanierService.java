package org.example.services;

import org.example.models.Article;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class PanierService {
    private static final PanierService INSTANCE = new PanierService();
    private final List<Article> items = new ArrayList<>();

    private PanierService() {
    }

    public static PanierService getInstance() {
        return INSTANCE;
    }

    public boolean ajouter(Article article) {
        if (article == null || contient(article)) {
            return false;
        }
        items.add(article);
        return true;
    }

    public void retirer(Article article) {
        if (article == null) {
            return;
        }
        items.removeIf(item -> memeArticle(item, article));
    }

    public void vider() {
        items.clear();
    }

    public List<Article> getItems() {
        return Collections.unmodifiableList(items);
    }

    public boolean contient(Article article) {
        if (article == null) {
            return false;
        }
        return items.stream().anyMatch(item -> memeArticle(item, article));
    }

    public int getCount() {
        return items.size();
    }

    public double getTotal() {
        double total = 0;
        for (Article article : items) {
            if (article != null && article.getPrix() != null) {
                total += article.getPrix();
            }
        }
        return total;
    }

    private boolean memeArticle(Article a, Article b) {
        if (a == null || b == null) {
            return false;
        }

        if (a.getId() > 0 && b.getId() > 0) {
            return a.getId() == b.getId();
        }

        return Objects.equals(a.getTitre(), b.getTitre())
                && Objects.equals(a.getCategorie(), b.getCategorie())
                && Objects.equals(a.getPrix(), b.getPrix())
                && Objects.equals(a.getImageUrl(), b.getImageUrl())
                && a.getArtisanId() == b.getArtisanId();
    }
}

