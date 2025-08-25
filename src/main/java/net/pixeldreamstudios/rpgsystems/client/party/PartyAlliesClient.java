package net.pixeldreamstudios.rpgsystems.client.party;

import java.util.UUID;

public final class PartyAlliesClient {
    private PartyAlliesClient() {}
    public static boolean sameParty(UUID a, UUID b) {
        return ClientPartyHudData.isSameParty(a, b);
    }
}
