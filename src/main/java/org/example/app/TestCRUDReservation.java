package org.example.app;

import org.example.models.Reservation;
import org.example.services.ServiceReservation;

import java.time.LocalDateTime;

public class TestCRUDReservation {
    public static void main(String[] args) {
        try {
            ServiceReservation service = new ServiceReservation();
            LocalDateTime dt = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0);
            Reservation r = new Reservation(
                    1,
                    null,
                    dt,
                    2,
                    ServiceReservation.STATUT_EN_ATTENTE
            );
            service.ajouter(r);
            System.out.println("Reservation ajoutée avec succès.");
        } catch (Exception e) {
            System.err.println("Erreur TestCRUDReservation: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
