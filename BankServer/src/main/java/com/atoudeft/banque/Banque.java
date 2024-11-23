package com.atoudeft.banque;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class Banque implements Serializable {
    private String nom;
    private List<CompteClient> comptes;

    public Banque(String nom) {
        this.nom = nom;
        this.comptes = new ArrayList<>();
    }
    /**
     * Recherche un compte-client à partir de son numéro.
     *
     * @param numeroCompteClient le numéro du compte-client
     * @return le compte-client s'il a été trouvé. Sinon, retourne null
     */
    public CompteClient getCompteClient(String numeroCompteClient) {
        for (CompteClient compte : comptes) {
            if(compte.getNumCompteClient().equals(numeroCompteClient)){
                return compte;
            }
        }
        return null;
    }
    /**
     * Vérifier qu'un compte-bancaire appartient bien au compte-client.
     *
     * @param numeroCompteBancaire numéro du compte-bancaire
     * @param numeroCompteClient   numéro du compte-client
     * @return true si le compte-bancaire appartient au compte-client
     */
    public boolean appartientA(String numeroCompteBancaire, String numeroCompteClient) {
        return false;
    }

    /**
     * Effectue un dépot d'argent dans un compte-bancaire
     *
     * @param montant      montant à déposer
     * @param numeroCompte numéro du compte
     * @return true si le dépot s'est effectué correctement
     */
    public boolean deposer(double montant, String numeroCompte) {
        return false;
    }

    /**
     * Effectue un retrait d'argent d'un compte-bancaire
     *
     * @param montant      montant retiré
     * @param numeroCompte numéro du compte
     * @return true si le retrait s'est effectué correctement
     */
    public boolean retirer(double montant, String numeroCompte) {
        return false;
    }

    /**
     * Effectue un transfert d'argent d'un compte à un autre de la même banque
     *
     * @param montant             montant à transférer
     * @param numeroCompteInitial numéro du compte d'où sera prélevé l'argent
     * @param numeroCompteFinal   numéro du compte où sera déposé l'argent
     * @return true si l'opération s'est déroulée correctement
     */
    public boolean transferer(double montant, String numeroCompteInitial, String numeroCompteFinal) {
        return false;
    }

    /**
     * Effectue un paiement de facture.
     *
     * @param montant       montant de la facture
     * @param numeroCompte  numéro du compte bancaire d'où va se faire le paiement
     * @param numeroFacture numéro de la facture
     * @param description   texte descriptif de la facture
     * @return true si le paiement s'est bien effectuée
     */
    public boolean payerFacture(double montant, String numeroCompte, String numeroFacture, String description) {
        return false;
    }
    /**
     * Crée un nouveau compte-client avec un numéro et un nip et l'ajoute à la liste des comptes.
     *
     * @param numCompteClient numéro du compte-client à créer
     * @param nip             nip du compte-client à créer
     * @return true si le compte a été créé correctement, sinon retourne false.
     */
    public boolean ajouter(String numCompteClient, String nip) {

        // Vérifier que le numéro a entre 6 et 8 caractères et ne contient que des lettres majuscules et des chiffres.
        if (numCompteClient.length() < 6 || numCompteClient.length() > 8 || !numCompteClient.matches("[A-Z0-9]+")) {
            return false;
        }
        // Vérifier que le nip a entre 4 et 5 caractères et ne contient que des chiffres.
        if (nip.length() < 4 || nip.length() > 5 || !nip.matches("[0-9]+")) {
            return false;
        }
        // 1. Vérifier si le numéro du compte-client existe déjà
        if (compteExiste(numCompteClient)){
            return false; // Si le compte-client existe déjà, retourner false
        } else {
            // 2. Créer un nouveau compte-client avec le numéro et le NIP
            CompteClient nouveauCompte = new CompteClient(numCompteClient, nip);
            // 3. Générer un nouveau numéro de compte bancaire
            // 4. Vérifier si ce numéro existe déjà dans tous les comptes bancaires
            String numeroCompteBancaire = getNumCompteBancaireValide();
            // 5. Créer un compte-chèque avec ce numéro de compte bancaire
            CompteCheque compteCheque = new CompteCheque(numeroCompteBancaire, TypeCompte.CHEQUE);
            nouveauCompte.ajouter(compteCheque);
            // 6. Ajouter le compte-client à la liste des comptes de la banque
            this.comptes.add(nouveauCompte);
            // 7. Retourner true pour indiquer que l'ajout a réussi
            return true;
        }
    }

    /**
     * Retourne le numéro du compte-chèque d'un client à partir de son numéro de compte-client.
     *
     * @param numCompteClient numéro de compte-client
     * @return numéro du compte-chèque du client ayant le numéro de compte-client
     */
    public String getNumeroCompteParDefaut(String numCompteClient) {
        for (CompteClient client : comptes) {
            if (client.getNumero().equals(numCompteClient)) {
                for (CompteBancaire compte : client.getComptes()) {
                    if (compte.getType() == TypeCompte.CHEQUE) {
                        return compte.getNumero();

                    }
                }
            }
        }
        return null;
    }
    /**
     * Vérifie dans la liste de compte-client si le compte client spécifié existe et vérifie si ce compte
     * possède un compte bancaire du type spécifié.
     *
     * @param numeroCompteClient numéro du compte client.
     * @param typeCompte         type de compte que l'on souhaite vérifier.
     * @return true si le compte spécifié détient un compte bancaire du type spécifié.
     */
    public boolean verifSiDejaCompte(String numeroCompteClient, TypeCompte typeCompte) {
        if (this.comptes.contains(getCompteClient(numeroCompteClient))) {
            return getCompteClient(numeroCompteClient).verifTypeCompte(typeCompte);
        } else {
            return false;
        }
    }
    //Guillaume Chrétien-Richardson
    /**
     * Créer un numéro de compte-bancaire aléatoire, teste s'il existe déjà dans un compte-bancaire
     * d'un compte-client dans la base de donnée de la banque.
     * @return Un numéro de compte bancaire valide (unique).
     */
    public String getNumCompteBancaireValide() {
        boolean numCompteBancaireValide = true;
        String numCompteBancaire = null;

        do {
            numCompteBancaire = CompteBancaire.genereNouveauNumero();
            for (CompteClient compte : this.comptes) {
                for(CompteBancaire comptes : compte.getComptes()){
                    if (comptes.getNumero().equals(numCompteBancaire)) {
                        numCompteBancaireValide = false;
                    }
                }
            }
        } while (!numCompteBancaireValide);
        return numCompteBancaire;
    }
    //Guillaume Chrétien-Richardson
    /**
     * Vérifie si le compte existe dans la base de donnée de la banque à partir du numéro de compte-client.
     * @param numCompteClient numéro du compte client.
     * @return true si le compte est trouvé, false sinon.
     */
    public boolean compteExiste(String numCompteClient) {
        for (CompteClient compte : comptes) {
            if (compte.getNumCompteClient().equals(numCompteClient)) {
                return true;
            }
        }
        return false;
    }
    public List<CompteClient> getComptes(){
        return comptes;
    }
}


