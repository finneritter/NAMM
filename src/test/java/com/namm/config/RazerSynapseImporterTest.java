package com.namm.config;

import com.namm.model.ActionType;
import com.namm.model.Macro;
import com.namm.model.MacroStep;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RazerSynapseImporterTest {

	@TempDir
	Path tempDir;

	// --- Basic key events ---

	@Test
	void importFromFile_keyDownAndKeyUp_parsesCorrectly() throws IOException {
		File file = writeXml("keys.xml", """
				<Macro>
				  <Name>KeyTest</Name>
				  <KeyDown Key="0x41"/>
				  <KeyUp Key="0x41"/>
				</Macro>
				""");

		List<Macro> macros = RazerSynapseImporter.importFromFile(file);

		assertEquals(1, macros.size());
		assertEquals("KeyTest", macros.get(0).getName());
		List<MacroStep> steps = macros.get(0).getSteps();
		assertEquals(2, steps.size());
		assertEquals(ActionType.KEY_PRESS, steps.get(0).getActionType());
		assertEquals(65, steps.get(0).getKeyCode()); // GLFW_KEY_A
		assertFalse(steps.get(0).isMouse());
		assertEquals(ActionType.KEY_RELEASE, steps.get(1).getActionType());
		assertEquals(65, steps.get(1).getKeyCode());
	}

	@Test
	void importFromFile_alternateTagNames_keypress_keyrelease() throws IOException {
		File file = writeXml("alt.xml", """
				<Macro>
				  <Name>AltTags</Name>
				  <KeyPress Key="0x42"/>
				  <KeyRelease Key="0x42"/>
				</Macro>
				""");

		List<Macro> macros = RazerSynapseImporter.importFromFile(file);

		assertEquals(1, macros.size());
		List<MacroStep> steps = macros.get(0).getSteps();
		assertEquals(2, steps.size());
		assertEquals(ActionType.KEY_PRESS, steps.get(0).getActionType());
		assertEquals(66, steps.get(0).getKeyCode()); // B
		assertEquals(ActionType.KEY_RELEASE, steps.get(1).getActionType());
	}

	@Test
	void importFromFile_underscoreTagNames_key_down_key_up() throws IOException {
		File file = writeXml("underscore.xml", """
				<Macro>
				  <Name>UnderscoreTags</Name>
				  <key_down Key="0x43"/>
				  <key_up Key="0x43"/>
				</Macro>
				""");

		List<Macro> macros = RazerSynapseImporter.importFromFile(file);

		assertEquals(1, macros.size());
		assertEquals(2, macros.get(0).getSteps().size());
		assertEquals(ActionType.KEY_PRESS, macros.get(0).getSteps().get(0).getActionType());
		assertEquals(67, macros.get(0).getSteps().get(0).getKeyCode()); // C
	}

	// --- Mouse events ---

	@Test
	void importFromFile_mouseDownAndUp_parsesCorrectly() throws IOException {
		File file = writeXml("mouse.xml", """
				<Macro>
				  <Name>MouseTest</Name>
				  <MouseDown Button="1"/>
				  <MouseUp Button="1"/>
				</Macro>
				""");

		List<Macro> macros = RazerSynapseImporter.importFromFile(file);

		assertEquals(1, macros.size());
		List<MacroStep> steps = macros.get(0).getSteps();
		assertEquals(2, steps.size());
		assertEquals(ActionType.MOUSE_CLICK, steps.get(0).getActionType());
		assertEquals(0, steps.get(0).getKeyCode()); // Razer 1 -> GLFW 0 (left)
		assertTrue(steps.get(0).isMouse());
		assertEquals(ActionType.MOUSE_RELEASE, steps.get(1).getActionType());
	}

	@Test
	void importFromFile_mouseRightClick_mapsCorrectly() throws IOException {
		File file = writeXml("mouse-right.xml", """
				<Macro>
				  <Name>RightClick</Name>
				  <MouseDown Button="2"/>
				  <MouseUp Button="2"/>
				</Macro>
				""");

		List<Macro> macros = RazerSynapseImporter.importFromFile(file);

		assertEquals(1, macros.size());
		assertEquals(1, macros.get(0).getSteps().get(0).getKeyCode()); // right = 1
	}

	@Test
	void importFromFile_mouseMiddleClick_mapsCorrectly() throws IOException {
		File file = writeXml("mouse-middle.xml", """
				<Macro>
				  <Name>MiddleClick</Name>
				  <MouseDown Button="3"/>
				</Macro>
				""");

		List<Macro> macros = RazerSynapseImporter.importFromFile(file);

		assertEquals(2, macros.get(0).getSteps().get(0).getKeyCode()); // middle = 2
	}

	@Test
	void importFromFile_mouseNamedButtons_mapsCorrectly() throws IOException {
		File file = writeXml("mouse-named.xml", """
				<Macro>
				  <Name>NamedButtons</Name>
				  <MouseDown Button="left"/>
				  <MouseDown Button="right"/>
				  <MouseDown Button="middle"/>
				  <MouseDown Button="lbutton"/>
				  <MouseDown Button="rbutton"/>
				  <MouseDown Button="mbutton"/>
				</Macro>
				""");

		List<Macro> macros = RazerSynapseImporter.importFromFile(file);

		List<MacroStep> steps = macros.get(0).getSteps();
		assertEquals(6, steps.size());
		assertEquals(0, steps.get(0).getKeyCode()); // left
		assertEquals(1, steps.get(1).getKeyCode()); // right
		assertEquals(2, steps.get(2).getKeyCode()); // middle
		assertEquals(0, steps.get(3).getKeyCode()); // lbutton
		assertEquals(1, steps.get(4).getKeyCode()); // rbutton
		assertEquals(2, steps.get(5).getKeyCode()); // mbutton
	}

	@Test
	void importFromFile_mouseNoButton_defaultsToLeftClick() throws IOException {
		File file = writeXml("mouse-default.xml", """
				<Macro>
				  <Name>DefaultMouse</Name>
				  <MouseDown/>
				</Macro>
				""");

		List<Macro> macros = RazerSynapseImporter.importFromFile(file);

		assertEquals(0, macros.get(0).getSteps().get(0).getKeyCode()); // default = left
	}

	@Test
	void importFromFile_alternateMouseTagNames() throws IOException {
		File file = writeXml("mouse-alt.xml", """
				<Macro>
				  <Name>AltMouse</Name>
				  <mouse_down Button="1"/>
				  <mouse_up Button="1"/>
				  <MouseButtonDown Button="1"/>
				  <MouseButtonUp Button="1"/>
				</Macro>
				""");

		List<Macro> macros = RazerSynapseImporter.importFromFile(file);

		List<MacroStep> steps = macros.get(0).getSteps();
		assertEquals(4, steps.size());
		assertEquals(ActionType.MOUSE_CLICK, steps.get(0).getActionType());
		assertEquals(ActionType.MOUSE_RELEASE, steps.get(1).getActionType());
		assertEquals(ActionType.MOUSE_CLICK, steps.get(2).getActionType());
		assertEquals(ActionType.MOUSE_RELEASE, steps.get(3).getActionType());
	}

	// --- Delay events ---

	@Test
	void importFromFile_delay_parsesCorrectly() throws IOException {
		File file = writeXml("delay.xml", """
				<Macro>
				  <Name>DelayTest</Name>
				  <KeyDown Key="0x41"/>
				  <Delay Duration="200"/>
				  <KeyUp Key="0x41"/>
				</Macro>
				""");

		List<Macro> macros = RazerSynapseImporter.importFromFile(file);

		List<MacroStep> steps = macros.get(0).getSteps();
		assertEquals(3, steps.size());
		assertEquals(ActionType.DELAY, steps.get(1).getActionType());
		assertEquals(200, steps.get(1).getDelayMs());
	}

	@Test
	void importFromFile_delayAlternateTagNames() throws IOException {
		File file = writeXml("delay-alt.xml", """
				<Macro>
				  <Name>DelayAlt</Name>
				  <Wait Duration="100"/>
				  <Sleep Duration="200"/>
				</Macro>
				""");

		List<Macro> macros = RazerSynapseImporter.importFromFile(file);

		List<MacroStep> steps = macros.get(0).getSteps();
		assertEquals(2, steps.size());
		assertEquals(100, steps.get(0).getDelayMs());
		assertEquals(200, steps.get(1).getDelayMs());
	}

	@Test
	void importFromFile_delayAlternateAttributes() throws IOException {
		File file = writeXml("delay-attrs.xml", """
				<Macro>
				  <Name>DelayAttrs</Name>
				  <Delay duration="150"/>
				  <Delay Time="300"/>
				  <Delay time="400"/>
				  <Delay Ms="500"/>
				  <Delay ms="600"/>
				</Macro>
				""");

		List<Macro> macros = RazerSynapseImporter.importFromFile(file);

		List<MacroStep> steps = macros.get(0).getSteps();
		assertEquals(5, steps.size());
		assertEquals(150, steps.get(0).getDelayMs());
		assertEquals(300, steps.get(1).getDelayMs());
		assertEquals(400, steps.get(2).getDelayMs());
		assertEquals(500, steps.get(3).getDelayMs());
		assertEquals(600, steps.get(4).getDelayMs());
	}

	@Test
	void importFromFile_delayFromTextContent() throws IOException {
		File file = writeXml("delay-text.xml", """
				<Macro>
				  <Name>DelayText</Name>
				  <Delay>250</Delay>
				</Macro>
				""");

		List<Macro> macros = RazerSynapseImporter.importFromFile(file);

		assertEquals(1, macros.get(0).getSteps().size());
		assertEquals(250, macros.get(0).getSteps().get(0).getDelayMs());
	}

	@Test
	void importFromFile_delayBelowMinimum_clampedTo20() throws IOException {
		File file = writeXml("delay-small.xml", """
				<Macro>
				  <Name>SmallDelay</Name>
				  <Delay Duration="5"/>
				</Macro>
				""");

		List<Macro> macros = RazerSynapseImporter.importFromFile(file);

		// MacroStep.delay clamps to Math.max(20, ms) inside the importer
		assertEquals(20, macros.get(0).getSteps().get(0).getDelayMs());
	}

	@Test
	void importFromFile_delayZero_skipped() throws IOException {
		File file = writeXml("delay-zero.xml", """
				<Macro>
				  <Name>ZeroDelay</Name>
				  <Delay Duration="0"/>
				</Macro>
				""");

		List<Macro> macros = RazerSynapseImporter.importFromFile(file);

		// Zero delay is skipped (ms > 0 check)
		assertTrue(macros.isEmpty() || macros.get(0).getSteps().isEmpty());
	}

	// --- Key code mapping ---

	@Test
	void importFromFile_hexKeyCode_mappedCorrectly() throws IOException {
		File file = writeXml("hex.xml", """
				<Macro>
				  <Name>HexKeys</Name>
				  <KeyDown Key="0x20"/>
				  <KeyDown Key="0x0D"/>
				  <KeyDown Key="0x1B"/>
				</Macro>
				""");

		List<Macro> macros = RazerSynapseImporter.importFromFile(file);

		List<MacroStep> steps = macros.get(0).getSteps();
		assertEquals(3, steps.size());
		assertEquals(32, steps.get(0).getKeyCode());   // Space
		assertEquals(257, steps.get(1).getKeyCode());  // Enter
		assertEquals(256, steps.get(2).getKeyCode());  // Escape
	}

	@Test
	void importFromFile_decimalKeyCode_mappedCorrectly() throws IOException {
		File file = writeXml("decimal.xml", """
				<Macro>
				  <Name>DecimalKeys</Name>
				  <KeyDown Key="65"/>
				  <KeyDown Key="66"/>
				</Macro>
				""");

		List<Macro> macros = RazerSynapseImporter.importFromFile(file);

		List<MacroStep> steps = macros.get(0).getSteps();
		assertEquals(2, steps.size());
		assertEquals(65, steps.get(0).getKeyCode());  // A
		assertEquals(66, steps.get(1).getKeyCode());  // B
	}

	@Test
	void importFromFile_singleLetterCharKey_mappedCorrectly() throws IOException {
		// Single-character letter keys fall through to the NumberFormatException
		// handler which maps 'a'-'z'/'A'-'Z' via character arithmetic
		File file = writeXml("char.xml", """
				<Macro>
				  <Name>CharKeys</Name>
				  <KeyDown Key="a"/>
				  <KeyDown Key="Z"/>
				</Macro>
				""");

		List<Macro> macros = RazerSynapseImporter.importFromFile(file);

		List<MacroStep> steps = macros.get(0).getSteps();
		assertEquals(2, steps.size());
		assertEquals(65, steps.get(0).getKeyCode());  // 'a' -> A
		assertEquals(90, steps.get(1).getKeyCode());  // 'Z' -> Z
	}

	@Test
	void importFromFile_singleDigitCharKey_parsedAsDecimalNotCharacter() throws IOException {
		// A single digit like "5" is parsed by Integer.parseInt successfully
		// as decimal 5, which is NOT in the SCAN_TO_GLFW map -> returns -1 -> skipped
		File file = writeXml("digit-char.xml", """
				<Macro>
				  <Name>DigitChar</Name>
				  <KeyDown Key="5"/>
				</Macro>
				""");

		List<Macro> macros = RazerSynapseImporter.importFromFile(file);

		// Decimal 5 is not a Windows VK code in the map, so the step is skipped
		assertTrue(macros.isEmpty() || macros.get(0).getSteps().isEmpty());
	}

	@Test
	void importFromFile_decimalDigitKeyCodes_mappedCorrectly() throws IOException {
		// The correct way to specify digit keys is with Windows VK codes (0x30-0x39)
		File file = writeXml("digit-vk.xml", """
				<Macro>
				  <Name>DigitVK</Name>
				  <KeyDown Key="0x30"/>
				  <KeyDown Key="0x35"/>
				  <KeyDown Key="0x39"/>
				</Macro>
				""");

		List<Macro> macros = RazerSynapseImporter.importFromFile(file);

		List<MacroStep> steps = macros.get(0).getSteps();
		assertEquals(3, steps.size());
		assertEquals(48, steps.get(0).getKeyCode());  // 0
		assertEquals(53, steps.get(1).getKeyCode());  // 5
		assertEquals(57, steps.get(2).getKeyCode());  // 9
	}

	@Test
	void importFromFile_functionKeys_mappedCorrectly() throws IOException {
		File file = writeXml("fkeys.xml", """
				<Macro>
				  <Name>FuncKeys</Name>
				  <KeyDown Key="0x70"/>
				  <KeyDown Key="0x7B"/>
				</Macro>
				""");

		List<Macro> macros = RazerSynapseImporter.importFromFile(file);

		List<MacroStep> steps = macros.get(0).getSteps();
		assertEquals(2, steps.size());
		assertEquals(290, steps.get(0).getKeyCode()); // F1
		assertEquals(301, steps.get(1).getKeyCode()); // F12
	}

	@Test
	void importFromFile_unknownKeyCode_skipped() throws IOException {
		File file = writeXml("unknown.xml", """
				<Macro>
				  <Name>Unknown</Name>
				  <KeyDown Key="0xFF"/>
				</Macro>
				""");

		List<Macro> macros = RazerSynapseImporter.importFromFile(file);

		// Unknown key code maps to -1, which is skipped (keyCode >= 0)
		assertTrue(macros.isEmpty() || macros.get(0).getSteps().isEmpty());
	}

	@Test
	void importFromFile_alternateKeyAttributes() throws IOException {
		File file = writeXml("alt-attrs.xml", """
				<Macro>
				  <Name>AltAttrs</Name>
				  <KeyDown key="0x41"/>
				  <KeyDown VKey="0x42"/>
				  <KeyDown vkey="0x43"/>
				  <KeyDown KeyCode="0x44"/>
				  <KeyDown keycode="0x45"/>
				</Macro>
				""");

		List<Macro> macros = RazerSynapseImporter.importFromFile(file);

		List<MacroStep> steps = macros.get(0).getSteps();
		assertEquals(5, steps.size());
		assertEquals(65, steps.get(0).getKeyCode()); // A
		assertEquals(66, steps.get(1).getKeyCode()); // B
		assertEquals(67, steps.get(2).getKeyCode()); // C
		assertEquals(68, steps.get(3).getKeyCode()); // D
		assertEquals(69, steps.get(4).getKeyCode()); // E
	}

	@Test
	void importFromFile_keyCodeFromTextContent() throws IOException {
		File file = writeXml("text-key.xml", """
				<Macro>
				  <Name>TextKey</Name>
				  <KeyDown>0x41</KeyDown>
				</Macro>
				""");

		List<Macro> macros = RazerSynapseImporter.importFromFile(file);

		assertEquals(1, macros.get(0).getSteps().size());
		assertEquals(65, macros.get(0).getSteps().get(0).getKeyCode()); // A
	}

	// --- Macro naming ---

	@Test
	void importFromFile_macroNameFromElement() throws IOException {
		File file = writeXml("named.xml", """
				<Macro>
				  <Name>MyMacro</Name>
				  <KeyDown Key="0x41"/>
				</Macro>
				""");

		List<Macro> macros = RazerSynapseImporter.importFromFile(file);

		assertEquals("MyMacro", macros.get(0).getName());
	}

	@Test
	void importFromFile_macroNameFromLowercaseElement() throws IOException {
		File file = writeXml("named-lower.xml", """
				<Macro>
				  <name>LowerName</name>
				  <KeyDown Key="0x41"/>
				</Macro>
				""");

		List<Macro> macros = RazerSynapseImporter.importFromFile(file);

		assertEquals("LowerName", macros.get(0).getName());
	}

	@Test
	void importFromFile_macroNameFromAttribute() throws IOException {
		File file = writeXml("named-attr.xml", """
				<Macro name="AttrName">
				  <KeyDown Key="0x41"/>
				</Macro>
				""");

		List<Macro> macros = RazerSynapseImporter.importFromFile(file);

		assertEquals("AttrName", macros.get(0).getName());
	}

	@Test
	void importFromFile_noMacroName_defaultsToImportedMacro() throws IOException {
		File file = writeXml("unnamed.xml", """
				<Macro>
				  <KeyDown Key="0x41"/>
				</Macro>
				""");

		List<Macro> macros = RazerSynapseImporter.importFromFile(file);

		assertEquals("Imported Macro", macros.get(0).getName());
	}

	// --- Multiple macros ---

	@Test
	void importFromFile_multipleMacros_parsesAll() throws IOException {
		File file = writeXml("multi.xml", """
				<Macros>
				  <Macro>
				    <Name>First</Name>
				    <KeyDown Key="0x41"/>
				  </Macro>
				  <Macro>
				    <Name>Second</Name>
				    <KeyDown Key="0x42"/>
				  </Macro>
				</Macros>
				""");

		List<Macro> macros = RazerSynapseImporter.importFromFile(file);

		assertEquals(2, macros.size());
		assertEquals("First", macros.get(0).getName());
		assertEquals("Second", macros.get(1).getName());
	}

	@Test
	void importFromFile_lowercaseMacroTag() throws IOException {
		File file = writeXml("lowercase.xml", """
				<macros>
				  <macro>
				    <Name>LowerMacro</Name>
				    <KeyDown Key="0x41"/>
				  </macro>
				</macros>
				""");

		List<Macro> macros = RazerSynapseImporter.importFromFile(file);

		assertEquals(1, macros.size());
		assertEquals("LowerMacro", macros.get(0).getName());
	}

	// --- Root-level parsing (no Macro tag) ---

	@Test
	void importFromFile_noMacroTag_parsesFromRoot() throws IOException {
		File file = writeXml("rootmacro.xml", """
				<Events>
				  <KeyDown Key="0x41"/>
				  <Delay Duration="100"/>
				  <KeyUp Key="0x41"/>
				</Events>
				""");

		List<Macro> macros = RazerSynapseImporter.importFromFile(file);

		assertEquals(1, macros.size());
		assertEquals("rootmacro", macros.get(0).getName()); // name from filename
		assertEquals(3, macros.get(0).getSteps().size());
	}

	@Test
	void importFromFile_rootMacroName_stripsXmlExtension() throws IOException {
		File file = writeXml("MyCustomMacro.xml", """
				<Root>
				  <KeyDown Key="0x41"/>
				</Root>
				""");

		List<Macro> macros = RazerSynapseImporter.importFromFile(file);

		assertEquals("MyCustomMacro", macros.get(0).getName());
	}

	// --- Recursive element parsing ---

	@Test
	void importFromFile_nestedElements_parsedRecursively() throws IOException {
		File file = writeXml("nested.xml", """
				<Macro>
				  <Name>Nested</Name>
				  <Events>
				    <Group>
				      <KeyDown Key="0x41"/>
				      <KeyUp Key="0x41"/>
				    </Group>
				  </Events>
				</Macro>
				""");

		List<Macro> macros = RazerSynapseImporter.importFromFile(file);

		assertEquals(1, macros.size());
		assertEquals(2, macros.get(0).getSteps().size());
	}

	// --- Error handling ---

	@Test
	void importFromFile_malformedXml_returnsEmptyList() throws IOException {
		File file = writeXml("malformed.xml", "not xml at all <<<<");

		List<Macro> macros = RazerSynapseImporter.importFromFile(file);

		assertNotNull(macros);
		assertTrue(macros.isEmpty());
	}

	@Test
	void importFromFile_nonExistentFile_returnsEmptyList() {
		File file = new File(tempDir.toFile(), "nonexistent.xml");

		List<Macro> macros = RazerSynapseImporter.importFromFile(file);

		assertNotNull(macros);
		assertTrue(macros.isEmpty());
	}

	@Test
	void importFromFile_emptyMacro_skipped() throws IOException {
		File file = writeXml("empty-macro.xml", """
				<Macros>
				  <Macro>
				    <Name>Empty</Name>
				  </Macro>
				  <Macro>
				    <Name>HasSteps</Name>
				    <KeyDown Key="0x41"/>
				  </Macro>
				</Macros>
				""");

		List<Macro> macros = RazerSynapseImporter.importFromFile(file);

		// Empty macro is skipped; only the one with steps is kept
		assertEquals(1, macros.size());
		assertEquals("HasSteps", macros.get(0).getName());
	}

	@Test
	void importFromFile_invalidKeyCode_stepSkipped() throws IOException {
		File file = writeXml("invalid-key.xml", """
				<Macro>
				  <Name>Invalid</Name>
				  <KeyDown Key="not_a_number_or_char"/>
				  <KeyDown Key="0x41"/>
				</Macro>
				""");

		List<Macro> macros = RazerSynapseImporter.importFromFile(file);

		// First key is invalid (multi-char non-numeric), second is valid
		assertEquals(1, macros.size());
		assertEquals(1, macros.get(0).getSteps().size());
		assertEquals(65, macros.get(0).getSteps().get(0).getKeyCode());
	}

	@Test
	void importFromFile_emptyKeyAttribute_stepSkipped() throws IOException {
		File file = writeXml("empty-key.xml", """
				<Macro>
				  <Name>EmptyKey</Name>
				  <KeyDown Key=""/>
				  <KeyDown Key="0x41"/>
				</Macro>
				""");

		List<Macro> macros = RazerSynapseImporter.importFromFile(file);

		assertEquals(1, macros.size());
		// Empty key with empty text content -> skipped, only second step survives
		assertEquals(1, macros.get(0).getSteps().size());
	}

	@Test
	void importFromFile_invalidDelayValue_skipped() throws IOException {
		File file = writeXml("bad-delay.xml", """
				<Macro>
				  <Name>BadDelay</Name>
				  <Delay Duration="abc"/>
				  <KeyDown Key="0x41"/>
				</Macro>
				""");

		List<Macro> macros = RazerSynapseImporter.importFromFile(file);

		assertEquals(1, macros.size());
		// Invalid delay returns 0, which is skipped (ms > 0)
		assertEquals(1, macros.get(0).getSteps().size());
		assertEquals(ActionType.KEY_PRESS, macros.get(0).getSteps().get(0).getActionType());
	}

	// --- Security: XXE prevention ---

	@Test
	void importFromFile_xxeAttempt_rejected() throws IOException {
		File file = writeXml("xxe.xml", """
				<?xml version="1.0"?>
				<!DOCTYPE foo [
				  <!ENTITY xxe SYSTEM "file:///etc/passwd">
				]>
				<Macro>
				  <Name>&xxe;</Name>
				  <KeyDown Key="0x41"/>
				</Macro>
				""");

		List<Macro> macros = RazerSynapseImporter.importFromFile(file);

		// DOCTYPE is disallowed, so parsing fails and returns empty list
		assertNotNull(macros);
		assertTrue(macros.isEmpty());
	}

	// --- Complex macro ---

	@Test
	void importFromFile_complexMacro_parsedCorrectly() throws IOException {
		File file = writeXml("complex.xml", """
				<Macro>
				  <Name>Sprint Jump Attack</Name>
				  <KeyDown Key="0x11"/>
				  <KeyDown Key="0x57"/>
				  <Delay Duration="100"/>
				  <KeyDown Key="0x20"/>
				  <Delay Duration="50"/>
				  <MouseDown Button="1"/>
				  <Delay Duration="30"/>
				  <MouseUp Button="1"/>
				  <KeyUp Key="0x20"/>
				  <KeyUp Key="0x57"/>
				  <KeyUp Key="0x11"/>
				</Macro>
				""");

		List<Macro> macros = RazerSynapseImporter.importFromFile(file);

		assertEquals(1, macros.size());
		assertEquals("Sprint Jump Attack", macros.get(0).getName());

		List<MacroStep> steps = macros.get(0).getSteps();
		assertEquals(11, steps.size());

		// Ctrl down
		assertEquals(ActionType.KEY_PRESS, steps.get(0).getActionType());
		// W down (0x57 -> maps to GLFW 87 = W)
		assertEquals(ActionType.KEY_PRESS, steps.get(1).getActionType());
		// Delay 100
		assertEquals(ActionType.DELAY, steps.get(2).getActionType());
		assertEquals(100, steps.get(2).getDelayMs());
		// Space down
		assertEquals(ActionType.KEY_PRESS, steps.get(3).getActionType());
		assertEquals(32, steps.get(3).getKeyCode()); // Space
		// Mouse click
		assertEquals(ActionType.MOUSE_CLICK, steps.get(5).getActionType());
		assertTrue(steps.get(5).isMouse());
		// Mouse release
		assertEquals(ActionType.MOUSE_RELEASE, steps.get(7).getActionType());
	}

	// --- Special keys ---

	@Test
	void importFromFile_specialKeys_mappedCorrectly() throws IOException {
		File file = writeXml("special.xml", """
				<Macro>
				  <Name>SpecialKeys</Name>
				  <KeyDown Key="0x08"/>
				  <KeyDown Key="0x09"/>
				  <KeyDown Key="0x10"/>
				  <KeyDown Key="0x12"/>
				  <KeyDown Key="0x14"/>
				  <KeyDown Key="0x25"/>
				  <KeyDown Key="0x26"/>
				  <KeyDown Key="0x27"/>
				  <KeyDown Key="0x28"/>
				  <KeyDown Key="0x2D"/>
				  <KeyDown Key="0x2E"/>
				</Macro>
				""");

		List<Macro> macros = RazerSynapseImporter.importFromFile(file);

		List<MacroStep> steps = macros.get(0).getSteps();
		assertEquals(11, steps.size());
		assertEquals(259, steps.get(0).getKeyCode());  // Backspace
		assertEquals(258, steps.get(1).getKeyCode());  // Tab
		assertEquals(340, steps.get(2).getKeyCode());  // Shift
		assertEquals(342, steps.get(3).getKeyCode());  // Alt
		assertEquals(280, steps.get(4).getKeyCode());  // Caps Lock
		assertEquals(263, steps.get(5).getKeyCode());  // Left Arrow
		assertEquals(265, steps.get(6).getKeyCode());  // Up Arrow
		assertEquals(262, steps.get(7).getKeyCode());  // Right Arrow
		assertEquals(264, steps.get(8).getKeyCode());  // Down Arrow
		assertEquals(260, steps.get(9).getKeyCode());  // Insert
		assertEquals(261, steps.get(10).getKeyCode()); // Delete
	}

	@Test
	void importFromFile_punctuationKeys_mappedCorrectly() throws IOException {
		File file = writeXml("punct.xml", """
				<Macro>
				  <Name>Punctuation</Name>
				  <KeyDown Key="0xBA"/>
				  <KeyDown Key="0xBB"/>
				  <KeyDown Key="0xBC"/>
				  <KeyDown Key="0xBD"/>
				  <KeyDown Key="0xBE"/>
				  <KeyDown Key="0xBF"/>
				  <KeyDown Key="0xC0"/>
				  <KeyDown Key="0xDB"/>
				  <KeyDown Key="0xDC"/>
				  <KeyDown Key="0xDD"/>
				  <KeyDown Key="0xDE"/>
				</Macro>
				""");

		List<Macro> macros = RazerSynapseImporter.importFromFile(file);

		List<MacroStep> steps = macros.get(0).getSteps();
		assertEquals(11, steps.size());
		assertEquals(59, steps.get(0).getKeyCode());   // Semicolon
		assertEquals(61, steps.get(1).getKeyCode());   // Equals
		assertEquals(44, steps.get(2).getKeyCode());   // Comma
		assertEquals(45, steps.get(3).getKeyCode());   // Minus
		assertEquals(46, steps.get(4).getKeyCode());   // Period
		assertEquals(47, steps.get(5).getKeyCode());   // Slash
		assertEquals(96, steps.get(6).getKeyCode());   // Grave accent
		assertEquals(91, steps.get(7).getKeyCode());   // Left bracket
		assertEquals(92, steps.get(8).getKeyCode());   // Backslash
		assertEquals(93, steps.get(9).getKeyCode());   // Right bracket
		assertEquals(39, steps.get(10).getKeyCode());  // Apostrophe
	}

	// --- Negative delay value ---

	@Test
	void importFromFile_negativeDelay_skipped() throws IOException {
		File file = writeXml("neg-delay.xml", """
				<Macro>
				  <Name>NegDelay</Name>
				  <Delay Duration="-100"/>
				  <KeyDown Key="0x41"/>
				</Macro>
				""");

		List<Macro> macros = RazerSynapseImporter.importFromFile(file);

		assertEquals(1, macros.size());
		// Negative delay is skipped (ms > 0 check)
		assertEquals(1, macros.get(0).getSteps().size());
		assertEquals(ActionType.KEY_PRESS, macros.get(0).getSteps().get(0).getActionType());
	}

	// --- Mouse button edge: zero and negative ---

	@Test
	void importFromFile_mouseButtonZero_clampedToZero() throws IOException {
		File file = writeXml("mouse-zero.xml", """
				<Macro>
				  <Name>MouseZero</Name>
				  <MouseDown Button="0"/>
				</Macro>
				""");

		List<Macro> macros = RazerSynapseImporter.importFromFile(file);

		// Math.max(0, 0-1) = 0
		assertEquals(0, macros.get(0).getSteps().get(0).getKeyCode());
	}

	// --- Helper ---

	private File writeXml(String filename, String content) throws IOException {
		Path path = tempDir.resolve(filename);
		Files.writeString(path, content);
		return path.toFile();
	}
}
