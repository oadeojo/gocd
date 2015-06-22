package com.thoughtworks.go.config;

import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.ConfigReposConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.server.materials.ScmMaterialCheckoutService;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Created by tomzo on 6/18/15.
 */
public class GoRepoConfigDataSourceTest {
    private GoConfigPluginService configPluginService;
    private GoConfigWatchList configWatchList;
    private ScmMaterialCheckoutService checkoutService;
    private PartialConfigProvider plugin;

    private GoRepoConfigDataSource repoConfigDataSource;

    private BasicCruiseConfig cruiseConfig ;

    File folder = new File("dir");

    @Before
    public void SetUp()
    {
        configPluginService = mock(GoConfigPluginService.class);
        plugin = mock(PartialConfigProvider.class);
        when(configPluginService.partialConfigProviderFor(any(ConfigRepoConfig.class))).thenReturn(plugin);

        cruiseConfig = new BasicCruiseConfig();
        CachedFileGoConfig fileMock = mock(CachedFileGoConfig.class);
        when(fileMock.currentConfig()).thenReturn(cruiseConfig);

        configWatchList = new GoConfigWatchList(fileMock);
        checkoutService = mock(ScmMaterialCheckoutService.class);

        repoConfigDataSource = new GoRepoConfigDataSource(configWatchList,configPluginService,checkoutService);
    }


    @Test
    public void shouldCallPluginLoadOnCheckout_WhenMaterialInWatchList() throws Exception
    {
        ScmMaterialConfig material = new GitMaterialConfig("http://my.git");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(new ConfigRepoConfig(material,"myplugin")));
        configWatchList.onConfigChange(cruiseConfig);

        repoConfigDataSource.onCheckoutComplete(material,folder,"7a8f");

        verify(plugin,times(1)).Load(eq(folder),any(PartialConfigLoadContext.class));
    }

    //@Test
    public void shouldProvideParseContextWhenCallingPluginParse() throws Exception
    {
        ScmMaterialConfig material = new GitMaterialConfig("http://my.git");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(new ConfigRepoConfig(material,"myplugin")));
        configWatchList.onConfigChange(cruiseConfig);

        repoConfigDataSource.onCheckoutComplete(material,folder,"7a8f");

        verify(plugin,times(1)).Load(eq(folder),notNull(PartialConfigLoadContext.class));
    }

    @Test
    public void shouldNotCallPluginLoadOnCheckout_WhenMaterialNotInWatchList() throws Exception
    {
        ScmMaterialConfig material = new GitMaterialConfig("http://my.git");

        repoConfigDataSource.onCheckoutComplete(material,folder,"7a8f");

        verify(plugin,times(0)).Load(eq(folder),any(PartialConfigLoadContext.class));
    }

    @Test
    public void shouldReturnLatestPartialConfigForMaterial_WhenPartialExists() throws  Exception
    {
        ScmMaterialConfig material = new GitMaterialConfig("http://my.git");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(new ConfigRepoConfig(material,"myplugin")));
        configWatchList.onConfigChange(cruiseConfig);

        repoConfigDataSource.onCheckoutComplete(material,folder,"7a8f");

        assertNotNull(repoConfigDataSource.latestPartialConfigForMaterial(material));
    }

    @Test
    public void shouldThrowWhenGettingLatestPartialConfig_WhenPluginHasFailed() throws  Exception
    {
        // use broken plugin now
        when(configPluginService.partialConfigProviderFor(any(ConfigRepoConfig.class)))
                .thenReturn(new BrokenConfigPlugin());

        ScmMaterialConfig material = new GitMaterialConfig("http://my.git");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(new ConfigRepoConfig(material,"myplugin")));
        configWatchList.onConfigChange(cruiseConfig);

        repoConfigDataSource.onCheckoutComplete(material,folder,"7a8f");

        assertTrue(repoConfigDataSource.latestParseHasFailedForMaterial(material));

        try
        {
            repoConfigDataSource.latestPartialConfigForMaterial(material);
        }
        catch (BrokenConfigPluginException ex)
        {
            return;
        }
        fail("should have thrown BrokenConfigPluginException");
    }

    private class BrokenConfigPlugin implements PartialConfigProvider
    {
        @Override
        public PartialConfig Load(File configRepoCheckoutDirectory, PartialConfigLoadContext context) {
            throw new BrokenConfigPluginException();
        }
    }

    private class BrokenConfigPluginException extends RuntimeException {
    }

    @Test
    public void shouldThrowWhenGettingLatestPartialConfig_WhenInitializingPluginHasFailed() throws  Exception
    {
        when(configPluginService.partialConfigProviderFor(any(ConfigRepoConfig.class)))
                .thenThrow(new RuntimeException("Failed to initialize plugin"));

        ScmMaterialConfig material = new GitMaterialConfig("http://my.git");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(new ConfigRepoConfig(material,"myplugin")));
        configWatchList.onConfigChange(cruiseConfig);

        repoConfigDataSource.onCheckoutComplete(material,folder,"7a8f");

        assertTrue(repoConfigDataSource.latestParseHasFailedForMaterial(material));

        try
        {
            repoConfigDataSource.latestPartialConfigForMaterial(material);
        }
        catch (RuntimeException ex)
        {
            assertThat(ex.getMessage(),is("Failed to initialize plugin"));
        }
    }

    @Test
    public void shouldRemovePartialsWhenRemovedFromWatchList() throws Exception
    {
        ScmMaterialConfig material = new GitMaterialConfig("http://my.git");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(new ConfigRepoConfig(material,"myplugin")));
        configWatchList.onConfigChange(cruiseConfig);

        repoConfigDataSource.onCheckoutComplete(material,folder,"7a8f");
        assertNotNull(repoConfigDataSource.latestPartialConfigForMaterial(material));

        // we change current configuration
        ScmMaterialConfig othermaterial = new GitMaterialConfig("http://myother.git");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(new ConfigRepoConfig(othermaterial,"myplugin")));
        configWatchList.onConfigChange(cruiseConfig);

        assertNull(repoConfigDataSource.latestPartialConfigForMaterial(material));
    }

    @Test
    public void shouldListenForConfigRepoListChanged()
    {
        assertTrue(configWatchList.hasListener(repoConfigDataSource));
    }


}
