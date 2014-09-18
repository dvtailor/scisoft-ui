/*
 * Copyright 2012 Diamond Light Source Ltd.
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

package uk.ac.diamond.sda.meta.discriminator;

import uk.ac.diamond.scisoft.analysis.io.IMetadata;

public interface IMetadataDiscriminator {

	/**
	 * This method returns true if the metadata being presented can be processed by the page
	 * 
	 * @param metadata
	 * @return is the page can process the metadata
	 */
	public boolean isApplicableFor(IMetadata metadata);

}
