package com.appsflyer.donkey.server.route;

import static com.appsflyer.donkey.server.route.PathDescriptor.MatchType;

public class PathDescriptorImpl implements PathDescriptor {
  
  private final String value;
  private final MatchType matchType;
  
  PathDescriptorImpl(String value, MatchType matchType) {
    this.value = value;
    this.matchType = matchType;
  }
  
  PathDescriptorImpl(String value) {
    this(value, MatchType.SIMPLE);
  }
  
  @Override
  public String value() {
    return value;
  }
  
  @Override
  public MatchType matchType() {
    return matchType;
  }
  
}