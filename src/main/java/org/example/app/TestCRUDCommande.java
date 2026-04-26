package org.example.app;

import org.example.models.Commande;
import org.example.services.ServiceCommande;

import java.sql.SQLException;
import java.util.List;

public class TestCRUDCommande {
	public static void main(String[] args) {
		ServiceCommande service = new ServiceCommande();

		try {
			Commande commande = new Commande();
			commande.setAdresseLivraison("12 Rue de Carthage, Tunis");
			commande.setTelephone("+21620123456");
			commande.setModePaiement("carte");
			commande.setStatut("en_attente");
			commande.setTotal(125.50);
			commande.setClientId(1);

			service.ajouter(commande);
			System.out.println("Commande ajoutee avec succes.");

			List<Commande> commandes = service.afficher();
			System.out.println("Nombre de commandes : " + commandes.size());
			for (Commande c : commandes) {
				System.out.println("- " + c.getNumero() + " | " + c.getStatut() + " | " + c.getTotal());
			}
		} catch (SQLException e) {
			System.err.println("Erreur SQL : " + e.getMessage());
		} catch (Exception e) {
			System.err.println("Erreur : " + e.getMessage());
		}
	}
}
