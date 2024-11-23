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
        } else {this.getHistorique().add(new OperationDepot(montant,TypeOperation.DEPOT));
            solde = +montant;
            return true;
        }
    }

    @Override
    public boolean debiter(double montant) {
        if (montant <= 0) {
            return false;
        } else {this.getHistorique().add(new OperationRetrait(montant,TypeOperation.RETRAIT));
            solde = -montant;
            return true;
        }
    }

    @Override
    public boolean payerFacture(String numeroFacture, double montant, String description)
    {
        this.getHistorique().add(new OperationFacture(montant,TypeOperation.FACTURE,numeroFacture,description));
        return false;
    }

    @Override
    public boolean transferer(double montant, String numeroCompteDestinataire)
    {
        this.getHistorique().add(new OperationTransfer(montant,TypeOperation.TRANSFER,numeroCompteDestinataire));
        return false;
    }
}