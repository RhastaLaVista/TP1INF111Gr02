package com.atoudeft.banque;

public class CompteEpargne extends CompteBancaire{
    private final double PRELEVEMENT = 2.0;
    private final double LIMITE_INFERIEURE = 1000.0;
    private double tauxInterets;

    /**
     * Crée un compte bancaire.
     *
     * @param numero numéro du compte
     * @param type   type du compte
     */
    public CompteEpargne(String numero, TypeCompte type, double tauxInterets) {
        super(numero, type);
        this.tauxInterets = tauxInterets / 100;
    }
    // ajoute le montant au solde s’il est strictement positif. Sinon,
//retourne false;
    @Override
    public boolean crediter(double montant) {
        if(montant > 0){
            solde += montant; // jai mit solde en protected dans compteBancaire
            this.getHistorique().add(new OperationDepot(montant,TypeOperation.DEPOT));
            return true;
        } else {
            return false;
        }
    }
    //retire le montant du solde s’il est strictement positif et qu’il y a
    //assez de fonds. Sinon, retourne false. Si l’opération a réussi et qu’il y a moins
    //de 1000$ dans le compte avant l’opération, on prélève des frais de 2$.
    @Override
    public boolean debiter(double montant) {
        if(solde < LIMITE_INFERIEURE){
            solde = solde - PRELEVEMENT;
            if(montant > 0 && solde - montant > 0) {
                solde = solde - montant;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean payerFacture(String numeroFacture, double montant, String description) {
        if(debiter(montant)) {
            this.getHistorique().add(new OperationFacture(montant, com.atoudeft.banque.TypeOperation.FACTURE, numeroFacture, description));
            return true;
        }
        return false;
    }

    @Override
    public boolean transferer(double montant, String numeroCompteDestinataire) {
        if(debiter(montant)) {
            this.getHistorique().add(new OperationTransfer(montant, TypeOperation.TRANSFER, numeroCompteDestinataire));
            return true;
        }
        return false;
    }

    public void ajouterInteret(){
        solde = solde + (solde * tauxInterets);
    }

    public double getPRELEVEMENT() {
        return PRELEVEMENT;
    }
    public double getLIMITE_INFERIEURE(){
        return LIMITE_INFERIEURE;
    }
}
