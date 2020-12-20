/**
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.social.yahoo.module;

import java.util.Date;
import java.util.List;

import org.scribe.up.profile.yahoo.YahooProfile;

/**
 * @author Ruiu Gabriel Mihai (gabriel.ruiu@mail.com)
 */
@SuppressWarnings("unused")
public class Contacts extends YahooProfile {

	private static final long serialVersionUID = -6992734244311600461L;
	
	private int count;
    private String gender;
    private String guid;
    private int height;
    private String imageURL;
    private String nickname;
    private YahooProfile profile;
    private String profileUrl;
    private String size;
    private int start;
	private int total;
}
