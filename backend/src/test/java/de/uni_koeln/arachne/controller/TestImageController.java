package de.uni_koeln.arachne.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import javax.imageio.ImageIO;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.BufferedImageHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import de.uni_koeln.arachne.service.IIPService;
import de.uni_koeln.arachne.testconfig.TestData;
import de.uni_koeln.arachne.util.TypeWithHTTPStatus;

@RunWith(MockitoJUnitRunner.class)
public class TestImageController {

	@Mock
	private IIPService iipService;
	
	@InjectMocks
	private ImageController controller;
	
	private MockMvc mockMvc;
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setMessageConverters(new BufferedImageHttpMessageConverter(), new StringHttpMessageConverter()).build();

		final TestData testData = new TestData();

		when(iipService.getImage(eq(0l), anyInt(), anyInt()))
		.then(new Answer<TypeWithHTTPStatus<BufferedImage>>() {
			@Override
			public TypeWithHTTPStatus<BufferedImage> answer(InvocationOnMock invocation)
					throws Throwable {
				int width = invocation.getArgumentAt(1, int.class);
				int height = invocation.getArgumentAt(2, int.class);

				TypeWithHTTPStatus<BufferedImage> result = 
						new TypeWithHTTPStatus<BufferedImage>(testData.getScaledTestImageJPEG(width, height));
				return result;
			}
		}
				);
		when(iipService.getImage(eq(1l), anyInt(), anyInt()))
		.thenReturn(new TypeWithHTTPStatus<BufferedImage>(HttpStatus.NOT_FOUND));

		when(iipService.getImagePropertiesForZoomifyViewer(0l))
		.thenReturn(new TypeWithHTTPStatus<String>(testData.getZoomifyPropertiesXML()));
		when(iipService.getImagePropertiesForZoomifyViewer(1l))
		.thenReturn(new TypeWithHTTPStatus<String>(HttpStatus.NOT_FOUND));
		
		when(iipService.getImageForZoomifyViewer(eq(0l), anyInt(), anyInt(), anyInt()))
		.thenReturn(new TypeWithHTTPStatus<BufferedImage>(testData.getScaledTestImageJPEG(64, 64)));
		when(iipService.getImageForZoomifyViewer(eq(1l), anyInt(), anyInt(), anyInt()))
		.thenReturn(new TypeWithHTTPStatus<BufferedImage>(HttpStatus.NOT_FOUND));
		
		when(iipService.resolution_HIGH()).thenReturn(0);
		when(iipService.resolution_PREVIEW()).thenReturn(600);
		when(iipService.resolution_THUMBNAIL()).thenReturn(150);
		when(iipService.resolution_ICON()).thenReturn(50);
	}

	@Test
	public void testGetWidth() throws Exception {
		// working
		MvcResult result = mockMvc.perform(
				get("/image/width/0").param("width", "666")
				.accept(MediaType.IMAGE_JPEG))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.IMAGE_JPEG))
				.andReturn();

		BufferedImage resultImage = ImageIO.read(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()));

		assertEquals(666, resultImage.getWidth());

		// invalid id
		mockMvc.perform(
				get("/image/width/1").param("width", "666")
				.accept(MediaType.IMAGE_JPEG))
				.andExpect(status().is4xxClientError());
	}

	@Test
	public void testGetHeight() throws Exception {
		// working
		MvcResult result = mockMvc.perform(
				get("/image/height/0").param("height", "666")
				.accept(MediaType.IMAGE_JPEG))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.IMAGE_JPEG))
				.andReturn();

		BufferedImage resultImage = ImageIO.read(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()));

		assertEquals(666, resultImage.getHeight());

		// invalid id
		mockMvc.perform(
				get("/image/height/1").param("height", "666")
				.accept(MediaType.IMAGE_JPEG))
				.andExpect(status().is4xxClientError());
	}

	@Test
	public void testGetImage() throws Exception {
		// working
		MvcResult result = mockMvc.perform(
				get("/image/0")
				.accept(MediaType.IMAGE_JPEG))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.IMAGE_JPEG))
				.andReturn();

		BufferedImage resultImage = ImageIO.read(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()));

		assertEquals(240, resultImage.getWidth());
		assertEquals(236, resultImage.getHeight());

		// invalid id
		mockMvc.perform(
				get("/image/1")
				.accept(MediaType.IMAGE_JPEG))
				.andExpect(status().is4xxClientError());
	}
	
	@Test
	public void testGetPreviewImage() throws Exception {
		// working
		MvcResult result = mockMvc.perform(
				get("/image/preview/0")
				.accept(MediaType.IMAGE_JPEG))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.IMAGE_JPEG))
				.andReturn();

		BufferedImage resultImage = ImageIO.read(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()));

		assertEquals(600, resultImage.getWidth());
		assertEquals(600, resultImage.getHeight());

		// invalid id
		mockMvc.perform(
				get("/image/preview/1")
				.accept(MediaType.IMAGE_JPEG))
				.andExpect(status().is4xxClientError());
	}
	
	@Test
	public void testGetThumbnailImage() throws Exception {
		// working
		MvcResult result = mockMvc.perform(
				get("/image/thumbnail/0")
				.accept(MediaType.IMAGE_JPEG))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.IMAGE_JPEG))
				.andReturn();

		BufferedImage resultImage = ImageIO.read(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()));

		assertEquals(150, resultImage.getWidth());
		assertEquals(150, resultImage.getHeight());

		// invalid id
		mockMvc.perform(
				get("/image/thumbnail/1")
				.accept(MediaType.IMAGE_JPEG))
				.andExpect(status().is4xxClientError());
	}
	
	@Test
	public void testGetIconImage() throws Exception {
		// working
		MvcResult result = mockMvc.perform(
				get("/image/icon/0")
				.accept(MediaType.IMAGE_JPEG))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.IMAGE_JPEG))
				.andReturn();

		BufferedImage resultImage = ImageIO.read(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()));

		assertEquals(50, resultImage.getWidth());
		assertEquals(50, resultImage.getHeight());

		// invalid id
		mockMvc.perform(
				get("/image/icon/1")
				.accept(MediaType.IMAGE_JPEG))
				.andExpect(status().is4xxClientError());
	}
	
	@Test
	public void testGetImagePropertiesForZoomifyViewer() throws Exception {
		// working
		mockMvc.perform(
				get("/image/zoomify/0/ImageProperties.xml")
				.accept(MediaType.APPLICATION_XML))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_XML));
		
		// invlid id
		mockMvc.perform(
				get("/image/zoomify/1/ImageProperties.xml")
				.accept(MediaType.APPLICATION_XML))
				.andExpect(status().is4xxClientError());
	}
	
	@Test
	public void testGetImageForZoomifyViewer() throws Exception {
		// working
		mockMvc.perform(
				get("/image/zoomify/0/0-0-0.jpg")
				.accept(MediaType.IMAGE_JPEG))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.IMAGE_JPEG));
		
		// invlid id
		mockMvc.perform(
				get("/image/zoomify/1/0-0-0.jpg")
				.accept(MediaType.IMAGE_JPEG))
				.andExpect(status().is4xxClientError());
	}
}
