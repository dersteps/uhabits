/*
 * Copyright (C) 2017 Álinson Santos Xavier <isoron@gmail.com>
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Loop Habit Tracker is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.isoron.uhabits.core.ui.screens.habits.list;

import org.isoron.uhabits.*;
import org.isoron.uhabits.core.models.*;
import org.isoron.uhabits.core.preferences.*;
import org.isoron.uhabits.core.utils.*;
import org.junit.*;
import org.mockito.*;

import java.io.*;

import static java.nio.file.Files.*;
import static junit.framework.TestCase.assertTrue;
import static org.apache.commons.io.FileUtils.*;
import static org.hamcrest.CoreMatchers.*;
import static org.isoron.uhabits.core.ui.screens.habits.list.ListHabitsBehavior.Message.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

public class ListHabitsBehaviorTest extends BaseUnitTest
{
    @Mock
    private ListHabitsBehavior.System system;

    @Mock
    private Preferences prefs;

    private ListHabitsBehavior behavior;

    @Mock
    private ListHabitsBehavior.Screen screen;

    private Habit habit1, habit2;

    @Captor
    ArgumentCaptor<ListHabitsBehavior.NumberPickerCallback> picker;

    @Override
    @Before
    public void setUp()
    {
        super.setUp();
        habit1 = fixtures.createShortHabit();
        habit2 = fixtures.createNumericalHabit();
        habitList.add(habit1);
        habitList.add(habit2);
        clearInvocations(habitList);

        behavior = new ListHabitsBehavior(habitList, system, taskRunner, screen,
            commandRunner, prefs);
    }

    @Test
    public void testOnEdit()
    {
        behavior.onEdit(habit2, DateUtils.getStartOfToday());
        verify(screen).showNumberPicker(eq(0.1), eq("miles"), picker.capture());
        picker.getValue().onNumberPicked(100);
        assertThat(habit2.getCheckmarks().getTodayValue(), equalTo(100000));
    }

    @Test
    public void testOnExportCSV() throws Exception
    {
        File outputDir = createTempDirectory("CSV").toFile();
        when(system.getCSVOutputDir()).thenReturn(outputDir);
        behavior.onExportCSV();
        verify(screen).showSendFileScreen(any());
        assertThat(listFiles(outputDir, null, false).size(), equalTo(1));
        deleteDirectory(outputDir);
    }

    @Test
    public void testOnExportCSV_fail() throws Exception
    {
        File outputDir = createTempDirectory("CSV").toFile();
        outputDir.setWritable(false);
        when(system.getCSVOutputDir()).thenReturn(outputDir);
        behavior.onExportCSV();
        verify(screen).showMessage(COULD_NOT_EXPORT);
    }

    @Test
    public void testOnHabitClick()
    {
        behavior.onClickHabit(habit1);
        verify(screen).showHabitScreen(habit1);
    }

    @Test
    public void testOnHabitReorder()
    {
        Habit from = habit1;
        Habit to = habit2;
        behavior.onReorderHabit(from, to);
        verify(habitList).reorder(from, to);
    }

    @Test
    public void testOnRepairDB()
    {
        behavior.onRepairDB();
        verify(habitList).repair();
        verify(screen).showMessage(DATABASE_REPAIRED);
    }

    @Test
    public void testOnStartup_firstLaunch()
    {
        long today = DateUtils.getStartOfToday();

        when(prefs.isFirstRun()).thenReturn(true);
        behavior.onStartup();
        verify(prefs).setFirstRun(false);
        verify(prefs).updateLastHint(-1, today);
        verify(screen).showIntroScreen();
    }

    @Test
    public void testOnStartup_notFirstLaunch()
    {
        when(prefs.isFirstRun()).thenReturn(false);
        behavior.onStartup();
        verify(prefs).incrementLaunchCount();
    }

    @Test
    public void testOnToggle()
    {
        assertTrue(habit1.isCompletedToday());
        behavior.onToggle(habit1, DateUtils.getStartOfToday());
        assertFalse(habit1.isCompletedToday());
    }

    @Test
    public void testOnSendBugReport() throws IOException
    {
        when(system.getBugReport()).thenReturn("hello");
        behavior.onSendBugReport();
        verify(screen).showSendBugReportToDeveloperScreen("hello");

        when(system.getBugReport()).thenThrow(new IOException());
        behavior.onSendBugReport();
        verify(screen).showMessage(COULD_NOT_GENERATE_BUG_REPORT);

    }

}