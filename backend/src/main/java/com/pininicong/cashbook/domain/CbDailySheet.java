package com.pininicong.cashbook.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "cb_daily_sheet")
public class CbDailySheet {

    @Id
    private LocalDate sheetDate;

    @Column(length = 2000)
    private String scheduleNote = "";

    /** 하단 오늘의 메모 */
    @Column(length = 4000)
    private String dayMemo = "";

    public LocalDate getSheetDate() {
        return sheetDate;
    }

    public void setSheetDate(LocalDate sheetDate) {
        this.sheetDate = sheetDate;
    }

    public String getScheduleNote() {
        return scheduleNote;
    }

    public void setScheduleNote(String scheduleNote) {
        this.scheduleNote = scheduleNote;
    }

    public String getDayMemo() {
        return dayMemo;
    }

    public void setDayMemo(String dayMemo) {
        this.dayMemo = dayMemo;
    }
}
