package com.atoudeft.banque;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CompteClient implements Serializable {
    private String numero;
    private String nip;
    private List<CompteBancaire> comptes;

    /**
     * Crée un compte-client avec un numéro et un nip.
     *
     * @param numero le numéro du compte-client
     * @param nip le nip
     */
    public CompteClient(String numero, String nip) {
        this.numero = numero;
        this.nip = nip;
        comptes = new ArrayList<>();
    }


    // Getter pour le numéro de compte client
    public String getNumero() {
        return numero;
    }

    // Getter pour le nip
    public String getNip() {
        return nip;
    }

    // Getter pour accéder à la liste des comptes bancaires
    public List<CompteBancaire> getComptes() {
        return comptes;
    }

    /**
     *Chercheur de compte qui fouille comptes et retourne un index en prenant un numero de compte bancaire.
     *
     *@param numcompteActuel le numéro de compte que tu voudrais trouver dans la liste comptes
     *@return le numéro de l'index de sa position dans l'arraylist comptes.
     */
    public int choixBancaire(String numcompteActuel) {
        if (!comptes.isEmpty()) {
            for (CompteBancaire compte : comptes) {
                if (numcompteActuel.matches(compte.getNumero())) {
                    return comptes.indexOf(compte);
                }
            }
        }
        return 0;//Si il surgit une problème, Il va prendre le premier compte dans la liste pour l'instant.
        }

    /**
     * Ajoute un compte bancaire au compte-client.
     *
     * @param compte le compte bancaire
     * @return true si l'ajout est réussi
     */
    public boolean ajouter(CompteBancaire compte) {
        return this.comptes.add(compte);
    }

    public Object getNumCompteClient() {
        return this.numero;
    }

    //Guillaume Chrétien-Richardson
    /**
     * Vérifie si le compte client possède déjà un compte d'un type spécifique.
     * @param typeCompte le type de compte que l'on souhaite vérifier.
     * @return true si le compte contient déjà le type spécifié en paramètre, false dans le cas contraire
     */
    public boolean verifTypeCompte(TypeCompte typeCompte) {
        for (CompteBancaire compte : comptes) {
            if (compte.getType() == typeCompte) {
                return true;
            }
        }
        return false;
    }
    //Guillaume Chrétien-Richardson
    /**
     *
     * @param numCompteBancaire
     * @return
     */
    public boolean verifNumCompte(String numCompteBancaire) {
        for (CompteBancaire compte : comptes) {
            if (compte.getNumero().equals(numCompteBancaire)) {
                return true;
            }
        }
        return false;
    }
}