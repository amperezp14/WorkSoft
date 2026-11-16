package com.worksoft.company.domain.journal;

import static com.worksoft.company.domain.exception.BusinessException.Code.POSTING_REFERENCE_INVALID;

import com.worksoft.company.domain.exception.BusinessException;

public final class PostingRules {
  private PostingRules() {}

  public static void requireEligible(
      boolean accountActive, boolean allowsPosting, boolean hasChildren, boolean partyActive) {
    if (!accountActive || !allowsPosting || hasChildren || !partyActive)
      throw new BusinessException(POSTING_REFERENCE_INVALID);
  }
}
