/*******************************************************************************
 * Copyright (c) 2012 Obeo.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package fr.obeo.releng.targetplatform.internal.pde;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.internal.core.target.IUBundleContainer;

import fr.obeo.releng.targetplatform.Option;
import fr.obeo.releng.targetplatform.resolved.ResolvedLocation;
import fr.obeo.releng.targetplatform.resolved.ResolvedTargetPlatform;

/**
 * @author <a href="mailto:mikael.barbero@obeo.fr">Mikael Barbero</a>
 *
 */
public class PDETargetDefinitionGenerator {

	private final ITargetPlatformService targetPlatformService;

	public PDETargetDefinitionGenerator(ITargetPlatformService targetPlatformService) {
		this.targetPlatformService = targetPlatformService;
	}
	
	public ITargetDefinition generate(IProvisioningAgent agent, ResolvedTargetPlatform targetPlatform, IProgressMonitor monitor) throws ProvisionException, OperationCanceledException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, 100);
		IMetadataRepositoryManager metadataRepositoryManager = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
		
		subMonitor.worked(10);
		
		List<ResolvedLocation> locations = targetPlatform.getLocations();
		List<ITargetLocation> iuLocations = new ArrayList<ITargetLocation>();
		
		SubMonitor loopMonitor = subMonitor.newChild(90).setWorkRemaining(locations.size());
		for (ResolvedLocation location : locations) {
			List<IInstallableUnit> toBeInstalled = location.getIUToBeInstalled(metadataRepositoryManager, loopMonitor.newChild(1));
			int resolutionFlags = getResolutionFlags(location);
			ITargetLocation iuLocation = targetPlatformService.newIULocation(
					toBeInstalled.toArray(new IInstallableUnit[0]), 
					new URI[] {location.getURI()}, resolutionFlags);
			iuLocations.add(iuLocation);
		}
		
		ITargetDefinition pdeTargetDefinition = targetPlatformService.newTarget();
		pdeTargetDefinition.setName(targetPlatform.getName());
		pdeTargetDefinition.setTargetLocations(iuLocations.toArray(new ITargetLocation[0]));
		
		return pdeTargetDefinition;
	}

	private int getResolutionFlags(ResolvedLocation location) {
		int resolutionFlags = 0;
		for (Option option : location.getOptions()) {
			switch (option) {
				case INCLUDE_ALL_ENVIRONMENTS:
					resolutionFlags |= IUBundleContainer.INCLUDE_ALL_ENVIRONMENTS;
					break;
				case INCLUDE_CONFIGURE_PHASE:
					resolutionFlags |= IUBundleContainer.INCLUDE_CONFIGURE_PHASE;
					break;
				case INCLUDE_REQUIRED:
					resolutionFlags |= IUBundleContainer.INCLUDE_REQUIRED;
					break;
				case INCLUDE_SOURCE:
					resolutionFlags |= IUBundleContainer.INCLUDE_SOURCE;
					break;
			}
		}
		return resolutionFlags;
	}
}
