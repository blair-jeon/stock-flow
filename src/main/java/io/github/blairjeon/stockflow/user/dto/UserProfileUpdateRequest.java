package io.github.blairjeon.stockflow.user.dto;

public record UserProfileUpdateRequest(
        String lastName,
        String firstName,
        String phoneNumber,
        String email,
        String address
) {
    public boolean isEmpty(){
        return lastName==null
                && firstName==null
                && phoneNumber==null
                && email==null
                && address==null;
    }
}