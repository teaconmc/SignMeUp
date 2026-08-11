package org.teacon.exhibition_portal.components;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public record EPOperation(
        @SerializedName("commands") List<Item> operations
) {
    public static final EPOperation INSTANCE = new EPOperation(List.of());

    public record Item(
            @SerializedName("id") String id,
            @SerializedName("title") String title,
            @SerializedName("tooltip") String tooltip,
            @SerializedName("commands") List<String> commands
    ) {
    }

    public int size() {
        return operations.size();
    }
}
