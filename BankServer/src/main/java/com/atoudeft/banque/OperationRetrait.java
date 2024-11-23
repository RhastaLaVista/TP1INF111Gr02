package com.atoudeft.banque;

public class OperationRetrait extends Operation {
    public OperationRetrait(double montant, com.atoudeft.banque.TypeOperation type){
        super(montant, type);
    }

    @Override
    public String toString() {
        return "  DATE " + super.getDate() +
                "\n TYPE " + type +
                "\n MONTANT " + montant;
    }
}
