package org.craftercms.engine.targeting;

import java.util.List;

/**
 * Created by alfonsovasquez on 14/8/15.
 */
public interface CandidateTargetIdsResolver {

    List<String> getTargetIds(String targetId, String defaultTargetId);

}
