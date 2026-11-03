package com.example.splitsmart.service;

import com.example.splitsmart.exception.ValidationException;
import com.example.splitsmart.model.User;
import com.example.splitsmart.repository.UserRepository;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class AccountServiceTest {

    private static final String NAME = "Tommy Trojan";
    private static final String EMAIL = "tommy@usc.edu";
    private static final String USC_ID = "1234567890";
    private static final String PASSWORD = "fighton";
    private static final String UID = "uid-1";

    private UserRepository userRepository;
    private AccountService accountService;

    @Before
    public void setUp() {
        userRepository = mock(UserRepository.class);
        accountService = new AccountService(userRepository, new InputValidator());
    }

    @Test
    public void register_checksDuplicatesThenCreatesAccountThenSavesProfile() {
        when(userRepository.createAuthAccount(EMAIL, PASSWORD)).thenReturn(UID);

        accountService.register(NAME, EMAIL, USC_ID, PASSWORD);

        InOrder order = inOrder(userRepository);
        order.verify(userRepository).findByEmail(EMAIL);
        order.verify(userRepository).findByUscId(USC_ID);
        order.verify(userRepository).createAuthAccount(EMAIL, PASSWORD);
        order.verify(userRepository).save(any());
    }

    @Test
    public void register_savesAndReturnsUserWithTheAuthUid() {
        when(userRepository.createAuthAccount(EMAIL, PASSWORD)).thenReturn(UID);

        User returned = accountService.register(NAME, EMAIL, USC_ID, PASSWORD);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertSame(saved.getValue(), returned);
        assertEquals(UID, returned.getUserId());
        assertEquals(NAME, returned.getName());
        assertEquals(EMAIL, returned.getEmail());
        assertEquals(USC_ID, returned.getUscId());
    }

    @Test
    public void register_trimsNameAndEmail() {
        when(userRepository.createAuthAccount(EMAIL, PASSWORD)).thenReturn(UID);

        User returned = accountService.register("  " + NAME + " ", " " + EMAIL + "  ", USC_ID, PASSWORD);

        assertEquals(NAME, returned.getName());
        assertEquals(EMAIL, returned.getEmail());
    }

    @Test
    public void register_acceptsShortestAllowedPassword() {
        when(userRepository.createAuthAccount(EMAIL, "123456")).thenReturn(UID);

        accountService.register(NAME, EMAIL, USC_ID, "123456");

        verify(userRepository).save(any());
    }

    @Test
    public void register_duplicateEmail_isRejected() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(new User());

        assertThrows(ValidationException.class,
                () -> accountService.register(NAME, EMAIL, USC_ID, PASSWORD));

        verify(userRepository, never()).createAuthAccount(any(), any());
        verify(userRepository, never()).save(any());
    }

    @Test
    public void register_duplicateUscId_isRejected() {
        when(userRepository.findByUscId(USC_ID)).thenReturn(new User());

        assertThrows(ValidationException.class,
                () -> accountService.register(NAME, EMAIL, USC_ID, PASSWORD));

        verify(userRepository, never()).createAuthAccount(any(), any());
        verify(userRepository, never()).save(any());
    }

    @Test
    public void register_nonUscEmail_isRejected() {
        assertRegisterRejected(NAME, "tommy@gmail.com", USC_ID, PASSWORD);
    }

    @Test
    public void register_emptyEmail_isRejected() {
        assertRegisterRejected(NAME, "", USC_ID, PASSWORD);
        assertRegisterRejected(NAME, null, USC_ID, PASSWORD);
    }

    @Test
    public void register_wrongIdLength_isRejected() {
        assertRegisterRejected(NAME, EMAIL, "123456789", PASSWORD);
    }

    @Test
    public void register_emptyId_isRejected() {
        assertRegisterRejected(NAME, EMAIL, "", PASSWORD);
        assertRegisterRejected(NAME, EMAIL, null, PASSWORD);
    }

    @Test
    public void register_emptyName_isRejected() {
        assertRegisterRejected("", EMAIL, USC_ID, PASSWORD);
        assertRegisterRejected("   ", EMAIL, USC_ID, PASSWORD);
        assertRegisterRejected(null, EMAIL, USC_ID, PASSWORD);
    }

    @Test
    public void register_emptyPassword_isRejected() {
        assertRegisterRejected(NAME, EMAIL, USC_ID, "");
        assertRegisterRejected(NAME, EMAIL, USC_ID, null);
    }

    @Test
    public void register_passwordFirebaseWouldReject_isRejected() {
        assertRegisterRejected(NAME, EMAIL, USC_ID, "12345");
    }

    @Test
    public void login_returnsTheSignedInUser() {
        User user = new User(UID, NAME, EMAIL, USC_ID);
        when(userRepository.signIn(EMAIL, PASSWORD)).thenReturn(UID);
        when(userRepository.findById(UID)).thenReturn(user);

        assertSame(user, accountService.login(EMAIL, PASSWORD));
    }

    @Test
    public void login_trimsEmail() {
        User user = new User(UID, NAME, EMAIL, USC_ID);
        when(userRepository.signIn(EMAIL, PASSWORD)).thenReturn(UID);
        when(userRepository.findById(UID)).thenReturn(user);

        assertSame(user, accountService.login(" " + EMAIL + " ", PASSWORD));
    }

    @Test
    public void login_wrongEmailOrPassword_isRejected() {
        when(userRepository.signIn(EMAIL, PASSWORD)).thenReturn(null);

        assertThrows(ValidationException.class, () -> accountService.login(EMAIL, PASSWORD));

        verify(userRepository, never()).findById(any());
    }

    @Test
    public void login_emptyFields_areRejectedBeforeSigningIn() {
        assertThrows(ValidationException.class, () -> accountService.login("", PASSWORD));
        assertThrows(ValidationException.class, () -> accountService.login("   ", PASSWORD));
        assertThrows(ValidationException.class, () -> accountService.login(null, PASSWORD));
        assertThrows(ValidationException.class, () -> accountService.login(EMAIL, ""));
        assertThrows(ValidationException.class, () -> accountService.login(EMAIL, null));

        verifyNoInteractions(userRepository);
    }

    @Test
    public void login_accountWithNoProfile_isRejected() {
        when(userRepository.signIn(EMAIL, PASSWORD)).thenReturn(UID);
        when(userRepository.findById(UID)).thenReturn(null);

        assertThrows(ValidationException.class, () -> accountService.login(EMAIL, PASSWORD));
    }

    @Test
    public void logout_signsOut() {
        accountService.logout();

        verify(userRepository).signOut();
    }

    @Test
    public void getCurrentUser_returnsTheLoggedInUser() {
        User user = new User(UID, NAME, EMAIL, USC_ID);
        when(userRepository.getCurrentUserId()).thenReturn(UID);
        when(userRepository.findById(UID)).thenReturn(user);

        assertSame(user, accountService.getCurrentUser());
    }

    @Test
    public void getCurrentUser_isNullWhenNobodyIsLoggedIn() {
        when(userRepository.getCurrentUserId()).thenReturn(null);

        assertNull(accountService.getCurrentUser());

        verify(userRepository, never()).findById(any());
    }

    @Test
    public void updateName_savesTheTrimmedName() {
        User user = new User(UID, NAME, EMAIL, USC_ID);
        when(userRepository.findById(UID)).thenReturn(user);

        accountService.updateName(UID, "  Traveler ");

        verify(userRepository).update(user);
        assertEquals("Traveler", user.getName());
    }

    @Test
    public void updateName_emptyName_isRejected() {
        assertThrows(ValidationException.class, () -> accountService.updateName(UID, ""));
        assertThrows(ValidationException.class, () -> accountService.updateName(UID, "   "));
        assertThrows(ValidationException.class, () -> accountService.updateName(UID, null));

        verifyNoInteractions(userRepository);
    }

    @Test
    public void updateName_missingUser_isRejected() {
        when(userRepository.findById(UID)).thenReturn(null);

        assertThrows(ValidationException.class, () -> accountService.updateName(UID, "Traveler"));

        verify(userRepository, never()).update(any());
    }

    private void assertRegisterRejected(String name, String email, String uscId, String password) {
        assertThrows(ValidationException.class,
                () -> accountService.register(name, email, uscId, password));

        verifyNoInteractions(userRepository);
    }
}
