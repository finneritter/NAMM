package com.namm.config;

import com.namm.NammMod;
import com.namm.model.ActionType;
import com.namm.model.Macro;
import com.namm.model.MacroStep;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Imports Razer Synapse macro XML files and converts them to NAMM Macro format.
 * Supports both Synapse 3 and basic Synapse 4 XML exports.
 */
public class RazerSynapseImporter {

	// Razer scan code → GLFW key code mapping
	private static final Map<Integer, Integer> SCAN_TO_GLFW = new HashMap<>();

	static {
		// Letters (Razer uses Windows virtual key codes)
		for (int i = 0; i < 26; i++) {
			SCAN_TO_GLFW.put(0x41 + i, 65 + i); // A-Z → GLFW_KEY_A through GLFW_KEY_Z
		}
		// Numbers
		for (int i = 0; i < 10; i++) {
			SCAN_TO_GLFW.put(0x30 + i, 48 + i); // 0-9 → GLFW_KEY_0 through GLFW_KEY_9
		}
		// Function keys
		for (int i = 0; i < 12; i++) {
			SCAN_TO_GLFW.put(0x70 + i, 290 + i); // F1-F12 → GLFW_KEY_F1 through GLFW_KEY_F12
		}
		// Special keys
		SCAN_TO_GLFW.put(0x08, 259);  // Backspace
		SCAN_TO_GLFW.put(0x09, 258);  // Tab
		SCAN_TO_GLFW.put(0x0D, 257);  // Enter
		SCAN_TO_GLFW.put(0x10, 340);  // Shift (left)
		SCAN_TO_GLFW.put(0x11, 341);  // Control (left)
		SCAN_TO_GLFW.put(0x12, 342);  // Alt (left)
		SCAN_TO_GLFW.put(0x14, 280);  // Caps Lock
		SCAN_TO_GLFW.put(0x1B, 256);  // Escape
		SCAN_TO_GLFW.put(0x20, 32);   // Space
		SCAN_TO_GLFW.put(0x25, 263);  // Left Arrow
		SCAN_TO_GLFW.put(0x26, 265);  // Up Arrow
		SCAN_TO_GLFW.put(0x27, 262);  // Right Arrow
		SCAN_TO_GLFW.put(0x28, 264);  // Down Arrow
		SCAN_TO_GLFW.put(0x2D, 260);  // Insert
		SCAN_TO_GLFW.put(0x2E, 261);  // Delete
		SCAN_TO_GLFW.put(0xBA, 59);   // Semicolon
		SCAN_TO_GLFW.put(0xBB, 61);   // Equals
		SCAN_TO_GLFW.put(0xBC, 44);   // Comma
		SCAN_TO_GLFW.put(0xBD, 45);   // Minus
		SCAN_TO_GLFW.put(0xBE, 46);   // Period
		SCAN_TO_GLFW.put(0xBF, 47);   // Slash
		SCAN_TO_GLFW.put(0xC0, 96);   // Grave accent
		SCAN_TO_GLFW.put(0xDB, 91);   // Left bracket
		SCAN_TO_GLFW.put(0xDC, 92);   // Backslash
		SCAN_TO_GLFW.put(0xDD, 93);   // Right bracket
		SCAN_TO_GLFW.put(0xDE, 39);   // Apostrophe
	}

	/**
	 * Import macros from a Razer Synapse XML file.
	 * Returns a list of Macro objects, or an empty list on failure.
	 */
	public static List<Macro> importFromFile(File file) {
		List<Macro> result = new ArrayList<>();
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			// Security: disable external entities
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(file);
			doc.getDocumentElement().normalize();

			// Try to find macro elements — Synapse uses various tag names
			NodeList macroNodes = doc.getElementsByTagName("Macro");
			if (macroNodes.getLength() == 0) {
				macroNodes = doc.getElementsByTagName("macro");
			}
			if (macroNodes.getLength() == 0) {
				// Try treating the whole file as one macro
				Macro macro = parseMacroFromRoot(doc.getDocumentElement(), file.getName());
				if (macro != null && !macro.getSteps().isEmpty()) {
					result.add(macro);
				}
				return result;
			}

			for (int i = 0; i < macroNodes.getLength(); i++) {
				Element macroEl = (Element) macroNodes.item(i);
				Macro macro = parseMacroElement(macroEl);
				if (macro != null && !macro.getSteps().isEmpty()) {
					result.add(macro);
				}
			}
		} catch (Exception e) {
			NammMod.LOGGER.error("Failed to import Razer Synapse macro file: {}", file.getName(), e);
		}
		return result;
	}

	private static Macro parseMacroElement(Element macroEl) {
		Macro macro = new Macro();

		// Get name
		String name = getChildText(macroEl, "Name");
		if (name == null) name = getChildText(macroEl, "name");
		if (name == null) name = macroEl.getAttribute("name");
		if (name == null || name.isEmpty()) name = "Imported Macro";
		macro.setName(name);

		// Parse events/actions
		List<MacroStep> steps = new ArrayList<>();
		parseEvents(macroEl, steps);
		macro.setSteps(steps);

		return macro;
	}

	private static Macro parseMacroFromRoot(Element root, String fileName) {
		Macro macro = new Macro();
		String name = fileName.replaceAll("\\.(xml|XML)$", "");
		macro.setName(name);

		List<MacroStep> steps = new ArrayList<>();
		parseEvents(root, steps);
		macro.setSteps(steps);

		return macro;
	}

	private static void parseEvents(Element parent, List<MacroStep> steps) {
		NodeList children = parent.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			if (!(children.item(i) instanceof Element el)) continue;

			String tagName = el.getTagName().toLowerCase();

			switch (tagName) {
				case "keydown", "keypress", "key_down" -> {
					int keyCode = parseKeyCode(el);
					if (keyCode >= 0) {
						steps.add(MacroStep.keyAction(ActionType.KEY_PRESS, keyCode, false));
					}
				}
				case "keyup", "keyrelease", "key_up" -> {
					int keyCode = parseKeyCode(el);
					if (keyCode >= 0) {
						steps.add(MacroStep.keyAction(ActionType.KEY_RELEASE, keyCode, false));
					}
				}
				case "mousedown", "mouse_down", "mousebuttondown" -> {
					int button = parseMouseButton(el);
					if (button >= 0) {
						steps.add(MacroStep.keyAction(ActionType.MOUSE_CLICK, button, true));
					}
				}
				case "mouseup", "mouse_up", "mousebuttonup" -> {
					int button = parseMouseButton(el);
					if (button >= 0) {
						steps.add(MacroStep.keyAction(ActionType.MOUSE_RELEASE, button, true));
					}
				}
				case "delay", "wait", "sleep" -> {
					int ms = parseDelay(el);
					if (ms > 0) {
						steps.add(MacroStep.delay(Math.max(20, ms)));
					}
				}
				default -> {
					// Recursively check child elements for events
					parseEvents(el, steps);
				}
			}
		}
	}

	private static int parseKeyCode(Element el) {
		// Try various attribute names
		String val = el.getAttribute("Key");
		if (val.isEmpty()) val = el.getAttribute("key");
		if (val.isEmpty()) val = el.getAttribute("VKey");
		if (val.isEmpty()) val = el.getAttribute("vkey");
		if (val.isEmpty()) val = el.getAttribute("KeyCode");
		if (val.isEmpty()) val = el.getAttribute("keycode");
		if (val.isEmpty()) val = el.getTextContent().trim();

		if (val.isEmpty()) return -1;

		try {
			int razerCode = val.startsWith("0x") ? Integer.parseInt(val.substring(2), 16) : Integer.parseInt(val);
			return SCAN_TO_GLFW.getOrDefault(razerCode, -1);
		} catch (NumberFormatException e) {
			// Try mapping single character
			if (val.length() == 1) {
				char c = Character.toUpperCase(val.charAt(0));
				if (c >= 'A' && c <= 'Z') return 65 + (c - 'A');
				if (c >= '0' && c <= '9') return 48 + (c - '0');
			}
			return -1;
		}
	}

	private static int parseMouseButton(Element el) {
		String val = el.getAttribute("Button");
		if (val.isEmpty()) val = el.getAttribute("button");
		if (val.isEmpty()) val = el.getTextContent().trim();

		if (val.isEmpty()) return 0; // default to left click

		try {
			int button = Integer.parseInt(val);
			// Razer: 1=left, 2=right, 3=middle → GLFW: 0=left, 1=right, 2=middle
			return Math.max(0, button - 1);
		} catch (NumberFormatException e) {
			return switch (val.toLowerCase()) {
				case "left", "lbutton" -> 0;
				case "right", "rbutton" -> 1;
				case "middle", "mbutton" -> 2;
				default -> 0;
			};
		}
	}

	private static int parseDelay(Element el) {
		String val = el.getAttribute("Duration");
		if (val.isEmpty()) val = el.getAttribute("duration");
		if (val.isEmpty()) val = el.getAttribute("Time");
		if (val.isEmpty()) val = el.getAttribute("time");
		if (val.isEmpty()) val = el.getAttribute("Ms");
		if (val.isEmpty()) val = el.getAttribute("ms");
		if (val.isEmpty()) val = el.getTextContent().trim();

		if (val.isEmpty()) return 0;

		try {
			return Integer.parseInt(val);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static String getChildText(Element parent, String tagName) {
		NodeList nodes = parent.getElementsByTagName(tagName);
		if (nodes.getLength() > 0) {
			return nodes.item(0).getTextContent().trim();
		}
		return null;
	}
}
