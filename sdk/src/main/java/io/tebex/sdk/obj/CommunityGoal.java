package io.tebex.sdk.obj;

import com.google.gson.JsonObject;
import io.tebex.sdk.util.StringUtil;
import lombok.Data;
import lombok.Getter;

import java.time.ZonedDateTime;

@Data
public class CommunityGoal {
    private final int id;
    private final ZonedDateTime createdAt;
    private final ZonedDateTime updatedAt;
    private final int accountId;
    private final String name;
    private final String description;
    private final String image;
    private final double target;
    private final double current;
    private final boolean repeatable;
    private final ZonedDateTime lastAchieved;
    private final int timesAchieved;
    private final Status status;
    private final boolean sale;

    public enum Status {
        ACTIVE,
        COMPLETED,
        DISABLED
    }

    public static CommunityGoal fromJsonObject(JsonObject jsonObject) {
        return new CommunityGoal(
                jsonObject.get("id").getAsInt(),
                StringUtil.toLegacyDate(jsonObject.get("created_at").getAsString()),
                StringUtil.toLegacyDate(jsonObject.get("updated_at").getAsString()),
                jsonObject.get("account").getAsInt(),
                jsonObject.get("name").getAsString(),
                jsonObject.get("description").getAsString(),
                !jsonObject.get("image").getAsString().isEmpty() ? jsonObject.get("image").getAsString() : null,
                jsonObject.get("target").getAsDouble(),
                jsonObject.get("current").getAsDouble(),
                jsonObject.get("repeatable").getAsInt() != 0,
                !jsonObject.get("last_achieved").isJsonNull() ? StringUtil.toLegacyDate(jsonObject.get("last_achieved").getAsString()) : null,
                jsonObject.get("times_achieved").getAsInt(),
                CommunityGoal.Status.valueOf(jsonObject.get("status").getAsString().toUpperCase()),
                jsonObject.get("sale").getAsBoolean()
        );
    }
}