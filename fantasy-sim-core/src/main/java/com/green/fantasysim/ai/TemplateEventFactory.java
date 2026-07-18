package com.green.fantasysim.ai;

import com.green.fantasysim.domain.Choice;
import com.green.fantasysim.domain.EventCard;
import com.green.fantasysim.engine.EventContext;

import java.util.ArrayList;
import java.util.List;

public final class TemplateEventFactory {
    private TemplateEventFactory(){}

    public static EventCard fallback(EventContext ctx, int salt) {
        EventCard c = new EventCard();
        c.id = "FALLBACK-" + ctx.day + "-" + salt;
        c.title = "[" + ctx.tags.get("region") + "/" + ctx.tags.get("threat") + "] 급변하는 징조";
        c.situation = "현장은 불안하다. 선택은 남아있고, 결과는 남는다.\n"
                + "위험도 " + ctx.tags.get("difficulty") + " / 테마 " + ctx.tags.get("theme");

        c.tags = List.of(
                "region:" + ctx.tags.get("region"),
                "threat:" + ctx.tags.get("threat"),
                "theme:" + ctx.tags.get("theme"),
                "difficulty:" + ctx.tags.get("difficulty"),
                "deity:" + ctx.tags.get("deity"),
                "archdemon:" + ctx.tags.get("archdemon"),
                "demonTier:" + ctx.tags.get("demonTier"),
                "sin:" + ctx.tags.get("sin"),
                "site:" + ctx.tags.get("site"),
                "faithSide:" + ctx.tags.get("faithSide"),
                "politicalAxis:" + ctx.tags.get("politicalAxis"),
                "entryPolicy:" + ctx.tags.get("entryPolicy"),
                "playerJob:" + ctx.tags.get("playerJob"),
                "playerRace:" + ctx.tags.get("playerRace"),
                "playerOrigin:" + ctx.tags.get("playerOrigin")
        );

        c.choices = new ArrayList<>();
        c.choices.add(new Choice("GOOD", "사람을 살린다. 손해를 감수하더라도 질서를 세운다."));
        c.choices.add(new Choice("NEUTRAL", "피해를 줄이는 선에서 타협한다. 위험은 남는다."));
        c.choices.add(new Choice("EVIL", "이익을 우선한다. 뒷일은 누군가가 감당하겠지."));
        return c;
    }
}
