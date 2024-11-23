package com.atoudeft.banque;

public class CompteCheque extends com.atoudeft.banque.CompteBancaire {
    /**
     * Crée un compte cheque.
     *
     * @param numero numéro du compte
     * @param type   type du compte
     */
    public CompteCheque(String numero, com.atoudeft.banque.TypeCompte type) {
        super(numero, type);
    }

    @Override
    public boolean crediter(double montant) {
        if (montant <= 0) {
            return false;
        } else {
            solde = +montant;
            return true;
        }
    }

    @Override
    public boolean debiter(double montant) {
        if (montant <= 0) {
            return false;
        }
        solde = -montant;
        return true;
    }

    @Override
    public boolean payerFacture(String numeroFacture, double montant, String description) {
        return false;
    }

    @Override
    public boolean transferer(double montant, String numeroCompteDestinataire) {
        return false;
    }
}