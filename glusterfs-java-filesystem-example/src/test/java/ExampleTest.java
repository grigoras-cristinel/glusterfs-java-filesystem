import org.junit.Test;

import com.peircean.glusterfs.example.Example;

import junit.framework.TestCase;

/**
 * @author <a href="http://about.me/louiszuckerman">Louis Zuckerman</a>
 */
public class ExampleTest extends TestCase {

	@Test
	public void testGetProvider() {
		new Example().getProvider("gluster");
	}
}
